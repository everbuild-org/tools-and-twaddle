package org.everbuild.twaddle.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import java.net.InetSocketAddress
import java.net.SocketAddress
import java.util.concurrent.CompletionStage
import java.util.concurrent.atomic.AtomicBoolean

/** Immutable Minestom initialization configuration which has not been bound yet. */
class ServerBootstrap internal constructor(
    internal val auth: Auth,
) {
    fun withAuth(auth: Auth): ServerBootstrap = ServerBootstrap(auth)

    fun bind(port: Int): BoundServerBootstrap = bind("0.0.0.0", port)

    fun bind(host: String, port: Int): BoundServerBootstrap = bind(
        address = InetSocketAddress(host, port),
    )

    fun bind(address: SocketAddress): BoundServerBootstrap = BoundServerBootstrap(
        auth = auth,
        address = address,
    )

    companion object {
        fun defaults(): ServerBootstrap = ServerBootstrap(
            auth = Auth.Offline(),
        )
    }
}

/** Bootstrap configuration which is statically guaranteed to have a bind address. */
class BoundServerBootstrap internal constructor(
    internal val auth: Auth,
    internal val address: SocketAddress,
)

suspend fun startFoundry(
    block: suspend BeforeInitContext.() -> BoundServerBootstrap,
): RunningFoundry = startFoundry(MinestomLifecycle, block)

internal suspend fun startFoundry(
    lifecycle: ServerLifecycle,
    block: suspend BeforeInitContext.() -> BoundServerBootstrap,
): RunningFoundry {
    val plans = ModulePlanCollector()
    val installed = mutableListOf<InstalledModule<*>>()
    var minecraftServer: MinecraftServer? = null

    try {
        val bootstrap = coroutineScope {
            BeforeInitContext(
                phaseScope = this,
                plans = plans,
            ).block()
        }
        val frozenPlans = plans.freeze()
        val initializedServer = lifecycle.initialize(bootstrap.auth)
        minecraftServer = initializedServer

        coroutineScope {
            frozenPlans.forEach { plan ->
                plan.startInstallation(
                    phaseScope = this,
                    server = initializedServer,
                    installed = installed,
                )
            }
        }

        installed.forEach { it.requireActive() }
        lifecycle.start(initializedServer, bootstrap.address)
        return RunningFoundry.create(
            minecraftServer = initializedServer,
            lifecycle = lifecycle,
            installedModules = installed,
        )
    } catch (failure: Throwable) {
        withContext(NonCancellable) {
            installed.asReversed().forEach { module ->
                suppressCleanupFailure(failure) { module.uninstall() }
            }
            plans.snapshot().asReversed().forEach { plan ->
                suppressCleanupFailure(failure) { plan.discardPreparation() }
            }
            minecraftServer?.let {
                suppressCleanupFailure(failure) { lifecycle.stop(it) }
            }
        }
        throw failure
    }
}

suspend fun runFoundry(
    block: suspend BeforeInitContext.() -> BoundServerBootstrap,
) {
    val foundry = startFoundry(block)
    try {
        awaitCancellation()
    } finally {
        foundry.stop()
    }
}

fun runFoundryBlocking(
    block: suspend BeforeInitContext.() -> BoundServerBootstrap,
) = runBlocking {
    runFoundry(block)
}

/** A bound Foundry runtime which owns every installed module. */
class RunningFoundry private constructor(
    val minecraftServer: MinecraftServer,
    private val lifecycle: ServerLifecycle,
    private val installedModules: List<InstalledModule<*>>,
) {
    private val stopLock = Mutex()
    private val shutdown = CompletableDeferred<Unit>()
    private var stopped = false

    init {
        installedModules.forEach { module ->
            module.onUnexpectedFailure { failure ->
                CoroutineScope(Dispatchers.Default).launch {
                    stopAfterFailure(failure)
                }
            }
        }
    }

    suspend fun awaitShutdown() = shutdown.await()

    suspend fun stop() {
        stopInternal(null)?.let { throw it }
    }

    private suspend fun stopAfterFailure(failure: Throwable) {
        stopInternal(failure)
    }

    private suspend fun stopInternal(initialFailure: Throwable?): Throwable? = stopLock.withLock {
        if (stopped) return@withLock null
        stopped = true

        var failure = initialFailure
        withContext(NonCancellable) {
            installedModules.asReversed().forEach { module ->
                failure = captureCleanupFailure(failure) { module.uninstall() }
            }
            failure = captureCleanupFailure(failure) {
                lifecycle.stop(minecraftServer)
            }
        }

        if (failure == null) shutdown.complete(Unit)
        else shutdown.completeExceptionally(failure)
        failure
    }

    fun awaitShutdownAsync(): CompletionStage<Void> = foundryVoidStage {
        awaitShutdown()
    }

    fun stopAsync(): CompletionStage<Void> = foundryVoidStage {
        stop()
    }

    companion object {
        @JvmSynthetic
        internal fun create(
            minecraftServer: MinecraftServer,
            lifecycle: ServerLifecycle,
            installedModules: List<InstalledModule<*>>,
        ): RunningFoundry = RunningFoundry(
            minecraftServer = minecraftServer,
            lifecycle = lifecycle,
            installedModules = installedModules,
        )
    }
}

internal interface ServerLifecycle {
    fun initialize(auth: Auth): MinecraftServer
    fun start(server: MinecraftServer, address: SocketAddress)
    fun stop(server: MinecraftServer)
}

private object MinestomLifecycle : ServerLifecycle {
    override fun initialize(auth: Auth): MinecraftServer = MinecraftServer.init(auth)

    override fun start(server: MinecraftServer, address: SocketAddress) {
        server.start(address)
    }

    override fun stop(server: MinecraftServer) {
        MinecraftServer.stopCleanly()
    }
}

private suspend fun suppressCleanupFailure(
    primary: Throwable,
    cleanup: suspend () -> Unit,
) {
    try {
        cleanup()
    } catch (cleanupFailure: Throwable) {
        if (cleanupFailure !== primary) primary.addSuppressed(cleanupFailure)
    }
}

private suspend fun captureCleanupFailure(
    primary: Throwable?,
    cleanup: suspend () -> Unit,
): Throwable? = try {
    cleanup()
    primary
} catch (cleanupFailure: Throwable) {
    when {
        primary == null -> cleanupFailure
        primary === cleanupFailure -> primary
        else -> primary.apply { addSuppressed(cleanupFailure) }
    }
}

internal class ModulePlanCollector {
    private val plans = mutableListOf<ModulePlan>()
    private var frozen = false

    @Synchronized
    fun add(plan: ModulePlan) {
        check(!frozen) { "The Foundry installation plan is already frozen" }
        plans += plan
    }

    @Synchronized
    fun freeze(): List<ModulePlan> {
        frozen = true
        return plans.toList()
    }

    @Synchronized
    fun snapshot(): List<ModulePlan> = plans.toList()
}

internal sealed interface ModulePlan {
    fun startInstallation(
        phaseScope: kotlinx.coroutines.CoroutineScope,
        server: MinecraftServer,
        installed: MutableList<InstalledModule<*>>,
    )

    suspend fun discardPreparation()
}

internal class DirectModulePlan<A : Any>(
    private val module: DirectModule<A>,
    private val result: CompletableDeferred<A>,
) : ModulePlan {
    override fun startInstallation(
        phaseScope: kotlinx.coroutines.CoroutineScope,
        server: MinecraftServer,
        installed: MutableList<InstalledModule<*>>,
    ) {
        phaseScope.asyncInstall(result) {
            installModule(module, server).also { handle ->
                synchronized(installed) { installed += handle }
            }.api
        }
    }

    override suspend fun discardPreparation() = Unit
}

internal class PhasedModulePlan<A : Any>(
    private val prepared: Deferred<PreparedModule<A>>,
    private val result: CompletableDeferred<A>,
) : ModulePlan {
    private val installationStarted = AtomicBoolean()

    override fun startInstallation(
        phaseScope: kotlinx.coroutines.CoroutineScope,
        server: MinecraftServer,
        installed: MutableList<InstalledModule<*>>,
    ) {
        phaseScope.asyncInstall(result) {
            val preparedModule = prepared.await()
            installationStarted.set(true)
            preparedModule.install(server).also { handle ->
                synchronized(installed) { installed += handle }
            }.api
        }
    }

    override suspend fun discardPreparation() {
        if (!installationStarted.get()) {
            runCatching { prepared.await() }.getOrNull()?.discard()
        }
    }
}

private fun <A : Any> kotlinx.coroutines.CoroutineScope.asyncInstall(
    result: CompletableDeferred<A>,
    install: suspend () -> A,
) {
    async(start = kotlinx.coroutines.CoroutineStart.UNDISPATCHED) {
        try {
            result.complete(install())
        } catch (failure: Throwable) {
            result.completeExceptionally(failure)
            throw failure
        }
    }
}
