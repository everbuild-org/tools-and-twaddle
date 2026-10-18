package org.everbuild.twaddle.loot

import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class LootPool(
    val rolls: NumberProviderDeclaration<Int>,
    val entries: List<LootEntry>,
    val bonusRolls: NumberProviderDeclaration<Float>? = null,
    val predicates: List<ContextPredicate> = emptyList(),
    val modifiers: List<LootModifier> = emptyList(),
)
