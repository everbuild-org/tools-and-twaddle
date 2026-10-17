package org.everbuild.twaddle.datapack_primitives.context

import net.minestom.server.codec.Codec

/** Evaluates block-predicate fields against the block in the loot context. */
fun interface BlockPredicateMatcher {
    fun matches(predicate: Map<String, Codec.RawValue>): Boolean
}

val BlockPredicateContext = DataContextKey<BlockPredicateMatcher>("twaddle:block_predicate_context")

@JvmSynthetic
fun DataContext.withBlockPredicateMatcher(matcher: BlockPredicateMatcher) =
    withBehaviour(BlockPredicateContext, matcher)
