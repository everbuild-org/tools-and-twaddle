package org.everbuild.twaddle.datapack_primitives.predicate

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.datapack_primitives.context.ContextPredicateResolverContext
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

/** A named predicate reference used by the data-driven predicate registry. */
data class ContextPredicateReference(val key: Key) : ContextPredicate {
    override fun type(): Key = REFERENCE_TYPE

    override fun test(context: DataContext, random: Random): Boolean {
        val predicate = context[ContextPredicateResolverContext].resolve(key)
            ?: error("No context predicate is registered as $key")
        return predicate.test(context, random)
    }

    private companion object {
        val REFERENCE_TYPE = Key.key("minecraft:reference")
    }
}
