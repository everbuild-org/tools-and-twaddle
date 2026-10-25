package org.everbuild.twaddle.loot.modifier

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate

/** Opaque caller-defined representation of a loot modifier. */
interface LootModifier {
    fun type(): Key

    /** Context predicates that must pass before this modifier is applied. */
    val conditions: List<ContextPredicate> get() = emptyList()
}

/** Keeps a modifier's shared vanilla conditions separate from its type-specific data. */
data class LootModifierWithConditions(
    val modifier: LootModifier,
    override val conditions: List<ContextPredicate>,
) : LootModifier {
    override fun type() = modifier.type()
}

internal fun LootModifier.withoutConditions(): LootModifier =
    (this as? LootModifierWithConditions)?.modifier ?: this
