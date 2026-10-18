package org.everbuild.twaddle.datapack_primitives.context

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate

/** Resolves named context-predicate references during evaluation. */
fun interface ContextPredicateResolver {
    fun resolve(key: Key): ContextPredicate?
}

val ContextPredicateResolverContext = DataContextKey<ContextPredicateResolver>("twaddle:context_predicate_resolver")

@JvmSynthetic
fun DataContext.withContextPredicateResolver(resolver: ContextPredicateResolver) =
    withBehaviour(ContextPredicateResolverContext, resolver)
