package org.everbuild.twaddle.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.everbuild.twaddle.core.logging.logger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Default [TwaddleContext] implementation. */
class TwaddleContextImpl private constructor(
    parentContext: CoroutineContext,
    private val name: String,
    private val parent: TwaddleContextImpl?,
) : TwaddleContext {
    @JvmOverloads
    constructor(parentContext: CoroutineContext = EmptyCoroutineContext) : this(parentContext, "twaddle", null)

    private val lifetimeJob = Job(parentContext[Job.Key])
    private val coroutineName = parent
        ?.coroutineContext
        ?.get(CoroutineName.Key)
        ?.name
        ?.let { "$it/$name" }
        ?: name

    override val coroutineContext: CoroutineContext =
        parentContext + Dispatchers.Default + lifetimeJob + CoroutineName(coroutineName)

    private val stateLock = Any()
    private val shutdownActions = mutableListOf<suspend () -> Unit>()
    private val shutdownComplete = CompletableDeferred<Unit>()
    private var shuttingDown = false

    override fun <A : AutoCloseable> own(resource: A): A {
        val accepted = synchronized(stateLock) {
            if (shuttingDown) false
            else {
                shutdownActions += { resource.close() }
                true
            }
        }

        if (accepted) return resource

        val failure = IllegalStateException("TwaddleContext is shutting down")
        try {
            resource.close()
        } catch (closeFailure: Throwable) {
            failure.addSuppressed(closeFailure)
        }
        throw failure
    }

    private fun tree(): String =
        parent?.let { "${it.tree()}:$name" } ?: name

    override fun fork(name: String): TwaddleContext {
        require(name.isNotBlank()) { "Fork name must not be blank" }
        logger.debug("Installing ${tree()}:$name")

        val child = TwaddleContextImpl(coroutineContext, name, this)
        val accepted = synchronized(stateLock) {
            if (shuttingDown) false
            else {
                shutdownActions += { child.shutdown() }
                true
            }
        }

        if (accepted) return child

        child.lifetimeJob.cancel()
        throw IllegalStateException("TwaddleContext is shutting down")
    }

    override suspend fun shutdown() {
        val ownedActions = synchronized(stateLock) {
            if (shuttingDown) null
            else {
                shuttingDown = true
                shutdownActions.asReversed().toList().also {
                    shutdownActions.clear()
                }
            }
        }

        if (ownedActions == null) {
            shutdownComplete.await()
            return
        }

        var failure: Throwable? = null
        withContext(NonCancellable) {
            lifetimeJob.cancelAndJoin()
            ownedActions.forEach { shutdown ->
                try {
                    shutdown()
                } catch (shutdownFailure: Throwable) {
                    val previousFailure = failure
                    if (previousFailure == null) failure = shutdownFailure
                    else if (previousFailure !== shutdownFailure) previousFailure.addSuppressed(shutdownFailure)
                }
            }
        }

        val shutdownFailure = failure
        if (shutdownFailure == null) {
            shutdownComplete.complete(Unit)
        } else {
            shutdownComplete.completeExceptionally(shutdownFailure)
            throw shutdownFailure
        }
    }

    override fun shutdownNow() {
        runBlocking { shutdown() }
    }

    companion object {
        private val logger = logger()
    }
}