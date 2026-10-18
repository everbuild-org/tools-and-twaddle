package org.everbuild.twaddle.loot.modifier

import java.util.Random
import org.everbuild.twaddle.datapack_primitives.context.DataContext

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

/** Supplies the data codec for one registered loot-modifier type. */
interface LootModifierType {
    fun codec(
        modifierCodec: Codec<LootModifier>,
        predicateCodec: Codec<ContextPredicate>,
        intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out LootModifier>

    /** Applies this modifier to an item. Returning `null` discards the item. */
    fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack? =
        throw UnsupportedOperationException("Runtime behavior is not implemented for modifier ${modifier.type()}")
}
