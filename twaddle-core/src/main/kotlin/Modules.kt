package org.everbuild.twaddle.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.minestom.server.MinecraftServer
import java.util.concurrent.CompletionStage

/** A module whose complete installation happens after Minestom initialization. */
interface DirectModule<A : Any> : FoundryModule<A> {
    suspend fun install(context: DirectModuleLifecycleContext<A>): Nothing
}

/** A module which prepares before Minestom initialization and installs afterward. */
interface PhasedModule<A : Any> : FoundryModule<A> {
    suspend fun prepare(context: ModuleLifecycleContext<A>): Nothing
}

interface FoundryModule<A : Any>

fun <A : Any> directModule(
    install: suspend DirectModuleLifecycleContext<A>.() -> Nothing,
): DirectModule<A> = object : DirectModule<A> {
    override suspend fun install(context: DirectModuleLifecycleContext<A>): Nothing = context.install()
}

fun <A : Any> phasedModule(
    prepare: suspend ModuleLifecycleContext<A>.() -> Nothing,
): PhasedModule<A> = object : PhasedModule<A> {
    override suspend fun prepare(context: ModuleLifecycleContext<A>): Nothing = context.prepare()
}

/** Capabilities available while a module is installing after Minestom initialization. */
@FoundryDsl
class ModuleInstallContext internal constructor(
    val minecraftServer: MinecraftServer,
    override val coroutineContext: kotlin.coroutines.CoroutineContext,
) : CoroutineScope, AfterInitContext

/** A direct module lifecycle which publishes its API at a terminal suspension point. */
@FoundryDsl
class DirectModuleLifecycleContext<A : Any> internal constructor(
    private val minecraftServer: MinecraftServer,
    private val installedApi: CompletableDeferred<A>,
) {
    suspend fun installed(
        create: suspend ModuleInstallContext.() -> A,
    ): Nothing {
        val context = ModuleInstallContext(
            minecraftServer = minecraftServer,
            coroutineContext = currentCoroutineContext(),
        )
        installedApi.complete(context.create())
        awaitCancellation()
    }
}

/** The phase-safe context held by a phased module across Minestom initialization. */
@FoundryDsl
class ModuleLifecycleContext<A : Any> internal constructor(
    private val prepared: CompletableDeferred<Unit>,
    private val installRequest: Deferred<MinecraftServer>,
    private val installed: CompletableDeferred<A>,
) {
    suspend fun afterInit(
        install: suspend ModuleInstallContext.() -> A,
    ): Nothing {
        prepared.complete(Unit)
        val server = installRequest.await()
        val context = ModuleInstallContext(
            minecraftServer = server,
            coroutineContext = currentCoroutineContext(),
        )
        installed.complete(context.install())
        awaitCancellation()
    }
}

/** A successfully prepared phased module which has not yet been installed. */
class PreparedModule<A : Any> private constructor(
    private val lifecycle: Deferred<Nothing>,
    private val installRequest: CompletableDeferred<MinecraftServer>,
    private val installedApi: Deferred<A>,
) {
    private val stateLock = Mutex()
    private var state = PreparedState.PREPARED
    private val discardComplete = CompletableDeferred<Unit>()

    suspend fun install(server: MinecraftServer): InstalledModule<A> {
        stateLock.withLock {
            check(state == PreparedState.PREPARED) {
                "The prepared module has already been installed or discarded"
            }
            state = PreparedState.INSTALLING
            installRequest.complete(server)
        }

        return try {
            val api = installedApi.await()
            stateLock.withLock { state = PreparedState.INSTALLED }
            InstalledModule.create(api, lifecycle)
        } catch (failure: Throwable) {
            stateLock.withLock { state = PreparedState.FAILED }
            terminateAfterFailure(lifecycle, failure)
            throw failure
        }
    }

    suspend fun discard() {
        val shouldCancel = stateLock.withLock {
            when (state) {
                PreparedState.PREPARED -> {
                    state = PreparedState.DISCARDING
                    true
                }

                PreparedState.DISCARDING, PreparedState.DISCARDED -> false
                else -> error("An installing or installed module cannot be discarded")
            }
        }

        if (!shouldCancel) {
            discardComplete.await()
            return
        }

        try {
            terminateLifecycle(lifecycle)
            stateLock.withLock { state = PreparedState.DISCARDED }
            discardComplete.complete(Unit)
        } catch (failure: Throwable) {
            stateLock.withLock { state = PreparedState.DISCARDED }
            discardComplete.completeExceptionally(failure)
            throw failure
        }
    }

    fun installAsync(server: MinecraftServer): CompletionStage<InstalledModule<A>> = foundryStage {
        install(server)
    }

    fun discardAsync(): CompletionStage<Void> = foundryVoidStage {
        discard()
    }

    companion object {
        @JvmSynthetic
        internal fun <A : Any> create(
            lifecycle: Deferred<Nothing>,
            installRequest: CompletableDeferred<MinecraftServer>,
            installedApi: Deferred<A>,
        ): PreparedModule<A> = PreparedModule(lifecycle, installRequest, installedApi)
    }
}

/** An installed module API together with ownership of its lifecycle coroutine. */
class InstalledModule<A : Any> private constructor(
    val api: A,
    val completion: Deferred<Nothing>,
) {
    private val uninstallLock = Mutex()
    private val uninstallComplete = CompletableDeferred<Unit>()
    private var uninstalling = false

    suspend fun uninstall() {
        val shouldCancel = uninstallLock.withLock {
            if (uninstalling) false
            else {
                uninstalling = true
                true
            }
        }

        if (!shouldCancel) {
            uninstallComplete.await()
            return
        }

        try {
            terminateLifecycle(completion)
            uninstallComplete.complete(Unit)
        } catch (failure: Throwable) {
            uninstallComplete.completeExceptionally(failure)
            throw failure
        }
    }

    fun uninstallAsync(): CompletionStage<Void> = foundryVoidStage {
        uninstall()
    }

    suspend fun awaitCompletion(): Nothing = completion.await()

    internal suspend fun requireActive() {
        if (completion.isCompleted) completion.await()
    }

    internal fun onUnexpectedFailure(report: (Throwable) -> Unit) {
        completion.invokeOnCompletion { failure ->
            if (failure != null && failure !is CancellationException) {
                report(failure)
            }
        }
    }

    companion object {
        @JvmSynthetic
        internal fun <A : Any> create(
            api: A,
            lifecycle: Deferred<Nothing>,
        ): InstalledModule<A> = InstalledModule(api, lifecycle)
    }
}

suspend fun <A : Any> prepareModule(
    module: PhasedModule<A>,
): PreparedModule<A> {
    val prepared = CompletableDeferred<Unit>()
    val installRequest = CompletableDeferred<MinecraftServer>()
    val installedApi = CompletableDeferred<A>()
    val owner = detachedLifecycleScope()
    val lifecycle = owner.async(start = CoroutineStart.UNDISPATCHED) {
        val context = ModuleLifecycleContext(
            prepared = prepared,
            installRequest = installRequest,
            installed = installedApi,
        )

        try {
            module.prepare(context)
        } catch (failure: Throwable) {
            prepared.completeExceptionally(failure)
            installedApi.completeExceptionally(failure)
            throw failure
        }
    }

    try {
        prepared.await()
    } catch (failure: Throwable) {
        terminateAfterFailure(lifecycle, failure)
        throw failure
    }

    return PreparedModule.create(
        lifecycle = lifecycle,
        installRequest = installRequest,
        installedApi = installedApi,
    )
}

suspend fun <A : Any> installModule(
    module: DirectModule<A>,
    server: MinecraftServer,
): InstalledModule<A> {
    val installedApi = CompletableDeferred<A>()
    val owner = detachedLifecycleScope()
    val lifecycle = owner.async(start = CoroutineStart.UNDISPATCHED) {
        val context = DirectModuleLifecycleContext(
            minecraftServer = server,
            installedApi = installedApi,
        )

        try {
            module.install(context)
        } catch (failure: Throwable) {
            installedApi.completeExceptionally(failure)
            throw failure
        }
    }

    return try {
        InstalledModule.create(installedApi.await(), lifecycle)
    } catch (failure: Throwable) {
        terminateAfterFailure(lifecycle, failure)
        throw failure
    }
}

private suspend fun detachedLifecycleScope(): CoroutineScope {
    val callerContext = currentCoroutineContext()
    return CoroutineScope(callerContext.minusKey(Job) + SupervisorJob())
}

private enum class PreparedState {
    PREPARED,
    INSTALLING,
    INSTALLED,
    DISCARDING,
    DISCARDED,
    FAILED,
}

/** Marker for APIs which are available only after Minestom initialization. */
sealed interface AfterInitContext

private suspend fun terminateAfterFailure(
    lifecycle: Deferred<Nothing>,
    primary: Throwable,
) {
    try {
        terminateLifecycle(lifecycle)
    } catch (cleanupFailure: Throwable) {
        if (cleanupFailure !== primary) primary.addSuppressed(cleanupFailure)
    }
}

private suspend fun terminateLifecycle(lifecycle: Deferred<Nothing>) {
    withContext(NonCancellable) {
        lifecycle.cancel()
        try {
            lifecycle.await()
        } catch (_: CancellationException) {
            // Expected completion for a lifecycle whose terminal state is cancellation.
        }
    }
}
