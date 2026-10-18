package org.everbuild.twaddle.loot

import org.everbuild.twaddle.loot.modifier.LootModifier

/** A loot table whose entry, predicate, modifier, and number-provider models are supplied by its caller. */
data class LootTable(
    val type: String = "generic",
    val pools: List<LootPool> = emptyList(),
    val randomSequence: String? = null,
    val modifiers: List<LootModifier> = emptyList(),
)
