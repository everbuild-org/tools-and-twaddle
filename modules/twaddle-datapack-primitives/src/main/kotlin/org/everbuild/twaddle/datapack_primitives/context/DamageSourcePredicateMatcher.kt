package org.everbuild.twaddle.datapack_primitives.context

import net.minestom.server.codec.Codec

/** Evaluates a damage-source sub-predicate. */
fun interface DamageSourcePredicateMatcher {
    fun matches(predicate: Codec.RawValue): Boolean
}

val DamageSourcePredicateContext = DataContextKey<DamageSourcePredicateMatcher>("twaddle:damage_source_predicate_context")

@JvmSynthetic
fun DataContext.withDamageSourcePredicateMatcher(matcher: DamageSourcePredicateMatcher) =
    withBehaviour(DamageSourcePredicateContext, matcher)
