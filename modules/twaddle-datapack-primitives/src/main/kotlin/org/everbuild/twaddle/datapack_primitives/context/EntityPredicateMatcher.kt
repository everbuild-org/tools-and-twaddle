package org.everbuild.twaddle.datapack_primitives.context

/** Evaluates entity sub-predicates for a named loot-context target. */
fun interface EntityPredicateMatcher {
    fun matches(target: String, predicate: EntityPredicate): Boolean
}

val EntityPredicateContext = DataContextKey<EntityPredicateMatcher>("twaddle:entity_predicate_context")

@JvmSynthetic
fun DataContext.withEntityPredicateMatcher(matcher: EntityPredicateMatcher) =
    withBehaviour(EntityPredicateContext, matcher)

/** Whether the loot context's killer is a player. */
val KilledByPlayerContext = DataContextKey<Boolean>("twaddle:killed_by_player_context")

@JvmSynthetic
fun DataContext.withKilledByPlayer(killedByPlayer: Boolean) =
    withBehaviour(KilledByPlayerContext, killedByPlayer)
