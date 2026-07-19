@file:OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)

package org.everbuild.twaddle.core

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import java.net.SocketAddress
import java.util.concurrent.CompletionException

val lifecycleTests by testSuite("Lifecycle tests") {
    test("Direct module keeps lexical resources open until uninstall") {
        lateinit var resource: TestResource
        val installed = installModule(directModule<ResourceApi> {
            resource = TestResource()
            resource.use {
                installed { ResourceApi(it) }
            }
        }, MinecraftServer())

        resource.closed shouldBe false
        installed.api.resource shouldBe resource

        installed.uninstall()
        resource.closed shouldBe true
    }

    test("Unexpected module lifetime failure stops Foundry") {
        val events = mutableListOf<String>()
        val failureGate = CompletableDeferred<Unit>()
        val failure = IllegalStateException("lifetime failed")
        val lifecycle = RecordingServerLifecycle(events)
        val foundry = startFoundry(lifecycle) {
            install(directModule {
                installed {
                    launch {
                        failureGate.await()
                        throw failure
                    }
                    AApi()
                }
            })
            complete { it.bind(0) }
        }

        val shutdown = async { runCatching { foundry.awaitShutdown() } }
        failureGate.complete(Unit)
        testScope.advanceUntilIdle()

        shutdown.await().exceptionOrNull()?.message shouldBe failure.message
        events shouldBe listOf("init", "bind", "stop")
    }

    test("Standalone installed module exposes lifetime failure") {
        val failureGate = CompletableDeferred<Unit>()
        val failure = IllegalStateException("lifetime failed")
        val installed = installModule(directModule<AApi> {
            installed {
                launch {
                    failureGate.await()
                    throw failure
                }
                AApi()
            }
        }, MinecraftServer())

        val completion = async { runCatching { installed.awaitCompletion() } }
        failureGate.complete(Unit)
        testScope.advanceUntilIdle()

        completion.await().exceptionOrNull()?.message shouldBe failure.message
    }

    test("Direct standalone module publishes its API and uninstalls lifetime work") {
        val lifetimeStarted = CompletableDeferred<Unit>()
        val lifetimeFinished = CompletableDeferred<Unit>()
        val module = directModule {
            try {
                lifetimeStarted.complete(Unit)
                installed { AApi() }
            } finally {
                lifetimeFinished.complete(Unit)
            }
        }

        val installed = installModule(module, MinecraftServer())
        lifetimeStarted.await()

        installed.api::class shouldBe AApi::class
        installed.uninstall()
        installed.uninstall()
        lifetimeFinished.isCompleted shouldBe true
    }

    test("Concurrent uninstall callers all wait for lifecycle cleanup") {
        val cleanupStarted = CompletableDeferred<Unit>()
        val cleanupGate = CompletableDeferred<Unit>()
        val installed = installModule(directModule {
            try {
                installed { AApi() }
            } finally {
                cleanupStarted.complete(Unit)
                withContext(NonCancellable) { cleanupGate.await() }
            }
        }, MinecraftServer())

        val first = async { installed.uninstall() }
        cleanupStarted.await()
        val second = async { installed.uninstall() }
        testScope.runCurrent()

        first.isCompleted shouldBe false
        second.isCompleted shouldBe false

        cleanupGate.complete(Unit)
        testScope.advanceUntilIdle()
        first.isCompleted shouldBe true
        second.isCompleted shouldBe true
    }

    test("Phased standalone module holds preparation resources through installation") {
        val events = mutableListOf<String>()
        val module = phasedModule {
            try {
                events += "prepared"
                afterInit {
                    events += "installed"
                    AApi()
                }
            } finally {
                events += "cleaned"
            }
        }

        val prepared = prepareModule(module)
        events shouldBe listOf("prepared")

        val installed = prepared.install(MinecraftServer())
        events shouldBe listOf("prepared", "installed")

        installed.uninstall()
        events shouldBe listOf("prepared", "installed", "cleaned")
    }

    test("Discarding a phased module unwinds preparation and is idempotent") {
        val cleaned = CompletableDeferred<Unit>()
        val prepared = prepareModule(phasedModule<AApi> {
            try {
                afterInit { AApi() }
            } finally {
                cleaned.complete(Unit)
            }
        })

        prepared.discard()
        prepared.discard()

        cleaned.isCompleted shouldBe true
    }

    test("Prepared module installation is single-use") {
        val prepared = prepareModule(phasedModule<AApi> {
            afterInit { AApi() }
        })
        val installed = prepared.install(MinecraftServer())

        val secondInstall = runCatching {
            prepared.install(MinecraftServer())
        }

        (secondInstall.exceptionOrNull() is IllegalStateException) shouldBe true
        installed.uninstall()
    }

    test("Install and discard race has one winner") {
        val prepared = prepareModule(phasedModule<AApi> {
            afterInit { AApi() }
        })

        val install = async { runCatching { prepared.install(MinecraftServer()) } }
        val discard = async { runCatching { prepared.discard() } }
        testScope.advanceUntilIdle()

        val installResult = install.await()
        val discardResult = discard.await()
        listOf(installResult.isSuccess, discardResult.isSuccess).count { it } shouldBe 1
        installResult.getOrNull()?.uninstall()
    }

    test("Java preparation adapter preserves failure") {
        val failure = IllegalStateException("preparation failed")
        val stage = prepareModuleAsync(phasedModule<AApi> { throw failure })

        val result = runCatching { stage.toCompletableFuture().join() }

        (result.exceptionOrNull() is CompletionException) shouldBe true
        (result.exceptionOrNull()?.cause is IllegalStateException) shouldBe true
        result.exceptionOrNull()?.cause?.message shouldBe failure.message
    }

    test("Cancelling a Java preparation stage cancels module preparation") {
        val started = CompletableDeferred<Unit>()
        val cleaned = CompletableDeferred<Unit>()
        val stage = prepareModuleAsync(phasedModule<AApi> {
            try {
                started.complete(Unit)
                awaitCancellation()
            } finally {
                cleaned.complete(Unit)
            }
        }).toCompletableFuture()

        started.await()
        stage.cancel(true)
        cleaned.await()

        stage.isCancelled shouldBe true
    }

    test("Ignored root async work delays Minestom initialization") {
        val events = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val lifecycle = RecordingServerLifecycle(events)

        val starting = async {
            startFoundry(lifecycle) {
                async {
                    events += "work started"
                    gate.await()
                    events += "work finished"
                }
                complete { it.bind(0) }
            }
        }

        testScope.runCurrent()
        events shouldBe listOf("work started")

        gate.complete(Unit)
        testScope.advanceUntilIdle()
        events shouldBe listOf("work started", "work finished", "init", "bind")

        starting.await().stop()
    }

    test("Bootstrap configuration may await a before-init module") {
        val events = mutableListOf<String>()
        val lifecycle = RecordingServerLifecycle(events)

        val foundry = startFoundry(lifecycle) {
            val configuration = installBeforeInit {
                events += "configuration"
                25_565
            }
            complete { it.bind(configuration.await()) }
        }

        events shouldBe listOf("configuration", "init", "bind")
        foundry.stop()
    }

    test("Phased preparation finishes before init and installation finishes before bind") {
        val events = mutableListOf<String>()
        val lifecycle = RecordingServerLifecycle(events)

        val foundry = startFoundry(lifecycle) {
            install(phasedModule<AApi> {
                events += "prepare"
                afterInit {
                    events += "install"
                    AApi()
                }
            })
            complete { it.bind(0) }
        }

        events shouldBe listOf("prepare", "init", "install", "bind")
        foundry.stop()
        events shouldBe listOf("prepare", "init", "install", "bind", "stop")
    }

    test("Independent phased modules prepare concurrently") {
        val events = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val lifecycle = RecordingServerLifecycle(events)

        val starting = async {
            startFoundry(lifecycle) {
                install(gatedPhasedModule("A", events, gate))
                install(gatedPhasedModule("B", events, gate))
                complete { it.bind(0) }
            }
        }

        testScope.runCurrent()
        events shouldBe listOf("A preparing", "B preparing")

        gate.complete(Unit)
        testScope.advanceUntilIdle()
        events shouldBe listOf(
            "A preparing",
            "B preparing",
            "A prepared",
            "B prepared",
            "init",
            "A installed",
            "B installed",
            "bind",
        )
        starting.await().stop()
    }

    test("Ignored module references still delay bind") {
        val events = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val lifecycle = RecordingServerLifecycle(events)

        val starting = async {
            startFoundry(lifecycle) {
                install(directModule {
                    events += "installing"
                    gate.await()
                    installed {
                        events += "installed"
                        AApi()
                    }
                })
                complete { it.bind(0) }
            }
        }

        testScope.runCurrent()
        events shouldBe listOf("init", "installing")

        gate.complete(Unit)
        testScope.advanceUntilIdle()
        events shouldBe listOf("init", "installing", "installed", "bind")
        starting.await().stop()
    }

    test("Module references order dependent after-init installations") {
        val events = mutableListOf<String>()
        val lifecycle = RecordingServerLifecycle(events)

        val foundry = startFoundry(lifecycle) {
            val a = install(directModule<AApi> {
                installed {
                    events += "A"
                    AApi()
                }
            })
            install(directModule<BApi> {
                installed {
                    val installedA = a.await()
                    events += "B"
                    BApi(installedA)
                }
            })
            complete { it.bind(0) }
        }

        events shouldBe listOf("init", "A", "B", "bind")
        foundry.stop()
    }

    test("Installation failure prevents bind and uninstalls successful modules") {
        val events = mutableListOf<String>()
        val lifecycle = RecordingServerLifecycle(events)
        val failure = IllegalStateException("installation failed")

        val result = runCatching {
            startFoundry(lifecycle) {
                val a = install(directModule {
                    try {
                        installed {
                            events += "A installed"
                            AApi()
                        }
                    } finally {
                        events += "A cleaned"
                    }
                })
                install(directModule<BApi> {
                    installed {
                        a.await()
                        throw failure
                    }
                })
                complete { it.bind(0) }
            }
        }

        result.exceptionOrNull() shouldBe failure
        events shouldBe listOf("init", "A installed", "A cleaned", "stop")
    }

    test("Preparation failure prevents Minestom initialization") {
        val events = mutableListOf<String>()
        val lifecycle = RecordingServerLifecycle(events)
        val failure = IllegalStateException("preparation failed")

        val result = runCatching {
            startFoundry(lifecycle) {
                install(phasedModule<AApi> { throw failure })
                complete { it.bind(0) }
            }
        }

        result.exceptionOrNull() shouldBe failure
        events shouldBe emptyList()
    }

    test("Minestom initialization failure discards prepared modules") {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("init failed")
        val lifecycle = RecordingServerLifecycle(events, initializeFailure = failure)

        val result = runCatching {
            startFoundry(lifecycle) {
                install(phasedModule<AApi> {
                    try {
                        events += "prepared"
                        afterInit { AApi() }
                    } finally {
                        events += "discarded"
                    }
                })
                complete { it.bind(0) }
            }
        }

        result.exceptionOrNull() shouldBe failure
        events shouldBe listOf("prepared", "init", "discarded")
    }

    test("Installation failure cancels sibling installers") {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("installation failed")
        val lifecycle = RecordingServerLifecycle(events)

        val result = runCatching {
            startFoundry(lifecycle) {
                install(directModule<AApi> {
                    try {
                        events += "sibling started"
                        awaitCancellation()
                    } finally {
                        events += "sibling cancelled"
                    }
                })
                install(directModule<BApi> { throw failure })
                complete { it.bind(0) }
            }
        }

        result.exceptionOrNull() shouldBe failure
        events shouldBe listOf("init", "sibling started", "sibling cancelled", "stop")
    }

    test("Bind failure rolls back installed modules and stops Minestom") {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("bind failed")
        val lifecycle = RecordingServerLifecycle(events, startFailure = failure)

        val result = runCatching {
            startFoundry(lifecycle) {
                install(cleanupModule("A", events) { AApi() })
                complete { it.bind(0) }
            }
        }

        result.exceptionOrNull() shouldBe failure
        events shouldBe listOf("init", "A installed", "bind", "A cleaned", "stop")
    }

    test("Bind failure uninstalls phased modules without discarding them again") {
        val events = mutableListOf<String>()
        val failure = IllegalStateException("bind failed")
        val lifecycle = RecordingServerLifecycle(events, startFailure = failure)

        val result = runCatching {
            startFoundry(lifecycle) {
                install(phasedModule<AApi> {
                    try {
                        events += "prepared"
                        afterInit {
                            events += "installed"
                            AApi()
                        }
                    } finally {
                        events += "uninstalled"
                    }
                })
                complete { it.bind(0) }
            }
        }

        result.exceptionOrNull() shouldBe failure
        failure.suppressed.toList() shouldBe emptyList()
        events shouldBe listOf("prepared", "init", "installed", "bind", "uninstalled", "stop")
    }

    test("Normal shutdown uninstalls modules in reverse successful-install order") {
        val events = mutableListOf<String>()
        val lifecycle = RecordingServerLifecycle(events)

        val foundry = startFoundry(lifecycle) {
            val a = install(cleanupModule("A", events) { AApi() })
            install(directModule {
                try {
                    installed {
                        a.await()
                        events += "B installed"
                        BApi(AApi())
                    }
                } finally {
                    events += "B cleaned"
                }
            })
            complete { it.bind(0) }
        }

        foundry.stop()
        foundry.stop()

        events shouldBe listOf(
            "init",
            "A installed",
            "B installed",
            "bind",
            "B cleaned",
            "A cleaned",
            "stop",
        )
    }
}

private fun <A : Any> cleanupModule(
    name: String,
    events: MutableList<String>,
    api: () -> A,
): DirectModule<A> = directModule {
    try {
        installed {
            events += "$name installed"
            api()
        }
    } finally {
        events += "$name cleaned"
    }
}

private fun gatedPhasedModule(
    name: String,
    events: MutableList<String>,
    gate: CompletableDeferred<Unit>,
): PhasedModule<AApi> = phasedModule {
    events += "$name preparing"
    gate.await()
    events += "$name prepared"
    afterInit {
        events += "$name installed"
        AApi()
    }
}

private class RecordingServerLifecycle(
    private val events: MutableList<String>,
    private val startFailure: Throwable? = null,
    private val initializeFailure: Throwable? = null,
) : ServerLifecycle {
    override fun initialize(auth: Auth): MinecraftServer {
        events += "init"
        initializeFailure?.let { throw it }
        return MinecraftServer()
    }

    override fun start(server: MinecraftServer, address: SocketAddress) {
        events += "bind"
        startFailure?.let { throw it }
    }

    override fun stop(server: MinecraftServer) {
        events += "stop"
    }
}

private class AApi

private class ResourceApi(
    val resource: TestResource,
)

private class TestResource : AutoCloseable {
    var closed = false
        private set

    override fun close() {
        closed = true
    }
}

private class BApi(
    val a: AApi,
)
