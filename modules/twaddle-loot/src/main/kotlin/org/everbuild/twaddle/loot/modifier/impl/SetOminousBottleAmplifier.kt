package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class SetOminousBottleAmplifierLootModifier(val amplifier: NumberProviderDeclaration<Int>) : LootModifier {
    override fun type() = LootModifierKeys.SET_OMINOUS_BOTTLE_AMPLIFIER
}

internal class SetOminousBottleAmplifierLootModifierType : LootModifierType {
    override fun codec(
        modifierCodec: Codec<LootModifier>,
        predicateCodec: Codec<ContextPredicate>,
        intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ) = StructCodec.struct(
        "amplifier", intNumberProviderCodec, { (it as SetOminousBottleAmplifierLootModifier).amplifier },
        ::SetOminousBottleAmplifierLootModifier,
    )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val amplifier = (modifier as SetOminousBottleAmplifierLootModifier).amplifier.get(context, random)
        return item.with(DataComponents.OMINOUS_BOTTLE_AMPLIFIER, amplifier)
    }
}
