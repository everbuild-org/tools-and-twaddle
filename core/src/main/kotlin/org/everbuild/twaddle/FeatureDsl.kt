package org.everbuild.twaddle

import kotlinx.coroutines.future.future
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import java.util.concurrent.CompletionStage

class FeatureDependency<F : Any> internal constructor(val id: FeatureId<F>)

@TwaddleDslScope
class FeatureDslScope<F : Any> {
    internal val dependencies = linkedSetOf<FeatureId<*>>()
    internal var install: (suspend FeatureContext.() -> F)? = null

    fun <T : Any> require(id: FeatureId<T>): FeatureDependency<T> {
        dependencies += id
        return FeatureDependency(id)
    }

    fun install(block: suspend FeatureContext.() -> F) {
        check(install == null) {
            "The feature installer has already been declared"
        }
        install = block
    }

    @PublishedApi
    internal fun build(id: FeatureId<F>): DslFeatureProviderImpl<F> {
        val installer = checkNotNull(install) {
            "The feature does not declare an installer"
        }

        return DslFeatureProviderImpl(
            id = id,
            dependencies = dependencies.toSet(),
            installFn = installer,
        )
    }
}

internal class DslFeatureProviderImpl<F : Any>(
    override val id: FeatureId<F>,
    override val dependencies: Collection<FeatureId<*>>,
    val installFn: suspend FeatureContext.() -> F
) : FeatureProvider<F> {
    override fun install(cx: FeatureContext): CompletionStage<F> {
        return cx.featureCoroutineScope
            .future { installFn(cx) }
    }
}

inline fun <reified F : Any> feature(key: Key, scope: FeatureDslScope<F>.() -> Unit): FeatureProvider<F> {
    val provider = FeatureDslScope<F>()
    scope.invoke(provider)
    return provider.build(
        id = FeatureId(key, F::class.java),
    )
}

inline fun <reified F : Any> feature(@KeyPattern key: String, scope: FeatureDslScope<F>.() -> Unit): FeatureProvider<F> {
    return feature<F>(Key.key(key), scope)
}

inline fun <reified F : Any> feature(id: FeatureId<F>, scope: FeatureDslScope<F>.() -> Unit): FeatureProvider<F> {
    return feature<F>(id.key, scope)
}
