package org.everbuild.twaddle.datapack_primitives.context

import net.minestom.server.codec.Codec

/** Evaluates an item predicate against the tool in the loot context. */
fun interface ToolPredicateMatcher {
    fun matches(predicate: Codec.RawValue): Boolean
}

val ToolPredicateContext = DataContextKey<ToolPredicateMatcher>("twaddle:tool_predicate_context")

@JvmSynthetic
fun DataContext.withToolPredicateMatcher(matcher: ToolPredicateMatcher) =
    withBehaviour(ToolPredicateContext, matcher)
