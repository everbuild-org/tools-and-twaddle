package org.everbuild.twaddle.loot.modifier.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class EnchantedCountIncreaseLootModifier(val count: NumberProviderDeclaration<Float>, val enchantment: Key) : LootModifier {
    override fun type() = LootModifierKeys.ENCHANTED_COUNT_INCREASE
}

internal class EnchantedCountIncreaseLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("count", floatNumberProviderCodec, { (it as EnchantedCountIncreaseLootModifier).count }, "enchantment", Codec.KEY, { (it as EnchantedCountIncreaseLootModifier).enchantment }, ::EnchantedCountIncreaseLootModifier)!!
}
