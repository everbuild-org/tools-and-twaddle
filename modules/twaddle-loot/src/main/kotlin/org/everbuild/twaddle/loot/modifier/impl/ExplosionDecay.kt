package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import org.everbuild.twaddle.datapack_primitives.context.ExplosionRadiusContext

import org.everbuild.twaddle.datapack_primitives.context.DataContext

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data object ExplosionDecayLootModifier : LootModifier {
    override fun type() = LootModifierKeys.EXPLOSION_DECAY
}

internal class ExplosionDecayLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>): StructCodec<out LootModifier> =
        StructCodec.struct { ExplosionDecayLootModifier }

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack? {
        val radius = context[ExplosionRadiusContext] ?: return item
        val survivingItems = (0 until item.amount()).count { random.nextFloat() <= 1f / radius }
        return if (survivingItems == 0) null else item.withAmount(survivingItems)
    }
}
