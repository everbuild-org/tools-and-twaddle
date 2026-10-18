package org.everbuild.twaddle.loot.modifier.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class ApplyBonusLootModifier(val enchantment: Key, val formula: String) : LootModifier {
    override fun type() = LootModifierKeys.APPLY_BONUS
}

internal class ApplyBonusLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("enchantment", Codec.KEY, { (it as ApplyBonusLootModifier).enchantment }, "formula", Codec.STRING, { (it as ApplyBonusLootModifier).formula }, ::ApplyBonusLootModifier)!!
}
