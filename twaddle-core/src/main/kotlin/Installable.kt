package org.everbuild.twaddle.core

import kotlinx.coroutines.Deferred

/** A reference to a module API which becomes available after initialization. */
class ModuleRef<out A : Any> internal constructor(
    internal val result: Deferred<A>,
) {
    val isCompleted: Boolean
        get() = result.isCompleted
}

context(_: BeforeInitContext)
suspend fun <A : Any> BeforeInitModuleRef<A>.await(): A = result.await()

context(_: AfterInitContext)
suspend fun <A : Any> BeforeInitModuleRef<A>.await(): A = result.await()

context(_: AfterInitContext)
suspend fun <A : Any> ModuleRef<A>.await(): A = result.await()
