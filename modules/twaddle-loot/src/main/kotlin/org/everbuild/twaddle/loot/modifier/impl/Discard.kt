package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import org.everbuild.twaddle.datapack_primitives.context.DataContext

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data object DiscardLootModifier : LootModifier {
    override fun type() = LootModifierKeys.DISCARD
}

internal class DiscardLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>): StructCodec<out LootModifier> =
        StructCodec.struct { DiscardLootModifier }

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack? = null
}
