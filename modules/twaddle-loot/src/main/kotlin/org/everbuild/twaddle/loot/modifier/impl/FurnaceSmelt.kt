package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.loot.modifier.FurnaceSmeltContext
import net.minestom.server.item.ItemStack
import java.util.Random

data object FurnaceSmeltLootModifier : LootModifier {
    override fun type() = LootModifierKeys.FURNACE_SMELT
}

internal class FurnaceSmeltLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>): StructCodec<out LootModifier> =
        StructCodec.struct { FurnaceSmeltLootModifier }

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val result = context[FurnaceSmeltContext].result(item) ?: return item
        return result.withAmount(item.amount())
    }
}
