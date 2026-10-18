package org.everbuild.twaddle.loot.entry

import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier

data class LootEntry(
    val value: LootEntryData,
    val weight: Int = 1,
    val quality: Int = 0,
    val predicates: List<ContextPredicate> = emptyList(),
    val modifiers: List<LootModifier> = emptyList(),
)
