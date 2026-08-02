package org.everbuild.twaddle.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import net.minestom.server.MinecraftServer
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/** Work prepared before Minestom initialization and completed afterward. */
typealias AfterInit<A> = suspend (MinecraftServer) -> A

/** Owns resources and coroutine work which live for the complete server lifetime. */
class TwaddleContext @JvmOverloads constructor(
    parentContext: CoroutineContext = EmptyCoroutineContext
) : CoroutineScope {
    private val lifetimeJob = Job()

    override val coroutineContext: CoroutineContext =
        parentContext + Dispatchers.Default + lifetimeJob + CoroutineName("twaddle")

    private val stateLock = Any()
    private val resources = mutableListOf<AutoCloseable>()
    private val shutdownComplete = CompletableDeferred<Unit>()
    private var shuttingDown = false

    /** Transfers ownership of [resource] to this context. */
    fun <A : AutoCloseable> own(resource: A): A {
        val accepted = synchronized(stateLock) {
            if (shuttingDown) false
            else {
                resources += resource
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

    /** Cancels lifetime work and closes owned resources in reverse acquisition order. */
    suspend fun shutdown() {
        val ownedResources = synchronized(stateLock) {
            if (shuttingDown) null
            else {
                shuttingDown = true
                resources.asReversed().toList().also {
                    resources.clear()
                }
            }
        }

        if (ownedResources == null) {
            shutdownComplete.await()
            return
        }

        var failure: Throwable? = null
        withContext(NonCancellable) {
            lifetimeJob.cancelAndJoin()
            ownedResources.forEach { resource ->
                try {
                    resource.close()
                } catch (closeFailure: Throwable) {
                    val previousFailure = failure
                    if (previousFailure == null) failure = closeFailure
                    else if (previousFailure !== closeFailure) previousFailure.addSuppressed(closeFailure)
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

    fun shutdownNow() {
        runBlocking { shutdown() }
    }
}

suspend inline fun runTwaddleApplication(crossinline block: suspend (TwaddleContext) -> Unit) {
    coroutineScope {
        val context = TwaddleContext(this.coroutineContext)
        try {
            block(context)
        } finally {
            context.shutdown()
        }
    }
}