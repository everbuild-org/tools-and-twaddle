package org.everbuild.twaddle

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.future.future
import net.kyori.adventure.key.Key
import net.kyori.adventure.key.KeyPattern
import java.util.concurrent.CompletionStage

data class FeatureId<A : Any>(
    val key: Key,
    val facade: Class<A>
)

inline fun <reified A : Any> FeatureId(key: Key) = FeatureId(key, A::class.java)
inline fun <reified A : Any> FeatureId(@KeyPattern key: String) = FeatureId(Key.key(key), A::class.java)

interface FeatureProvider<A : Any> {
    val id: FeatureId<A>
    val dependencies: Collection<FeatureId<*>>

    fun install(cx: FeatureContext): CompletionStage<A>
}


