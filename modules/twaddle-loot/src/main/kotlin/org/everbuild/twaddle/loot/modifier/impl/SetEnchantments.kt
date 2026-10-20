package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class SetEnchantmentsLootModifier(val enchantments: Codec.RawValue, val add: Boolean) : LootModifier {
    override fun type() = LootModifierKeys.SET_ENCHANTMENTS
}

internal class SetEnchantmentsLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("enchantments", Codec.RAW_VALUE, { (it as SetEnchantmentsLootModifier).enchantments }, "add", Codec.BOOLEAN.optional(), { (it as SetEnchantmentsLootModifier).add.takeIf { add -> add } }, { enchantments, add -> SetEnchantmentsLootModifier(enchantments, add ?: false) })!!
}
