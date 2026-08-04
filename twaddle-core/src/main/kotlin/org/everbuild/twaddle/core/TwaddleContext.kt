package org.everbuild.twaddle.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import net.minestom.server.MinecraftServer

/** Work prepared before Minestom initialization and completed afterward. */
typealias AfterInit<A> = suspend (MinecraftServer) -> A

/** A lifetime which owns resources and coroutine work. */
interface TwaddleContext : CoroutineScope {
    fun initialized(server: MinecraftServer): TwaddleServerContext =
        TwaddleServerContext(server, this)

    /** Transfers ownership of [resource] to this context. */
    fun <A : AutoCloseable> own(resource: A): A

    /** Creates a named child lifetime which is shut down with this context. */
    fun fork(name: String): TwaddleContext

    fun <T> forked(name: String, block: (TwaddleContext) -> T): T =
        block(fork(name))

    /** Cancels lifetime work and shuts down owned lifetimes in reverse acquisition order. */
    suspend fun shutdown()

    fun shutdownNow()

    companion object {
        @JvmStatic
        fun root(): TwaddleContext = TwaddleContextImpl()
    }
}

suspend inline fun runTwaddleApplication(crossinline block: suspend (TwaddleContext) -> Unit) {
    coroutineScope {
        val context: TwaddleContext = TwaddleContextImpl(this.coroutineContext)
        try {
            block(context)
        } finally {
            context.shutdown()
        }
    }
}
