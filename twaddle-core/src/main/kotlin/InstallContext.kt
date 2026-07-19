package org.everbuild.twaddle.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

/** Capabilities available before Minestom initialization. */
@FoundryDsl
class BeforeInitContext internal constructor(
    private val phaseScope: CoroutineScope,
    private val plans: ModulePlanCollector,
) : CoroutineScope by phaseScope {
    fun <A : Any> install(module: DirectModule<A>): ModuleRef<A> {
        val result = CompletableDeferred<A>()
        plans.add(DirectModulePlan(module, result))
        return ModuleRef(result)
    }

    fun <A : Any> install(module: PhasedModule<A>): ModuleRef<A> {
        val result = CompletableDeferred<A>()
        val prepared = phaseScope.async(start = CoroutineStart.UNDISPATCHED) {
            prepareModule(module)
        }
        plans.add(PhasedModulePlan(prepared, result))
        return ModuleRef(result)
    }

    fun <A : Any> installBeforeInit(
        install: suspend BeforeInitContext.() -> A,
    ): BeforeInitModuleRef<A> = BeforeInitModuleRef(
        phaseScope.async(start = CoroutineStart.UNDISPATCHED) {
            this@BeforeInitContext.install()
        },
    )

    suspend fun complete(
        configure: suspend (ServerBootstrap) -> BoundServerBootstrap,
    ): BoundServerBootstrap = configure(ServerBootstrap.defaults())
}

class BeforeInitModuleRef<out A : Any> internal constructor(
    internal val result: Deferred<A>,
) {
    val isCompleted: Boolean
        get() = result.isCompleted
}
