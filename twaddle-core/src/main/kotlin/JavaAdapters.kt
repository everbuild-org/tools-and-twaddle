@file:JvmName("FoundryModules")

package org.everbuild.twaddle.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.await
import kotlinx.coroutines.future.future
import net.minestom.server.MinecraftServer
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage

/** Java-friendly bootstrap callback for the high-level Foundry lifecycle. */
fun interface FoundryBootstrap {
    fun configure(context: BeforeInitContext): CompletionStage<BoundServerBootstrap>
}

fun <A : Any> prepareModuleAsync(
    module: PhasedModule<A>,
): CompletionStage<PreparedModule<A>> = foundryStage {
    prepareModule(module)
}

fun <A : Any> installModuleAsync(
    module: DirectModule<A>,
    server: MinecraftServer,
): CompletionStage<InstalledModule<A>> = foundryStage {
    installModule(module, server)
}

fun startFoundryAsync(
    bootstrap: FoundryBootstrap,
): CompletionStage<RunningFoundry> = foundryStage {
    startFoundry {
        bootstrap.configure(this).await()
    }
}

fun runFoundryAsync(
    bootstrap: FoundryBootstrap,
): CompletionStage<Void> = foundryVoidStage {
    runFoundry {
        bootstrap.configure(this).await()
    }
}

@JvmSynthetic
internal fun <A> foundryStage(
    operation: suspend () -> A,
): CompletionStage<A> = CoroutineScope(Dispatchers.Default).future {
    operation()
}

@JvmSynthetic
internal fun foundryVoidStage(
    operation: suspend () -> Unit,
): CompletionStage<Void> {
    val source = foundryStage(operation).toCompletableFuture()
    val result = CompletableFuture<Void>()

    source.whenComplete { _, failure ->
        when {
            source.isCancelled -> result.cancel(false)
            failure != null -> result.completeExceptionally(failure)
            else -> result.complete(null)
        }
    }
    result.whenComplete { _, _ ->
        if (result.isCancelled) source.cancel(true)
    }
    return result
}
