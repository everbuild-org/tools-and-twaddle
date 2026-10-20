package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import org.everbuild.twaddle.datapack_primitives.context.DataContext

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.component.PotionContents
import net.minestom.server.potion.PotionType
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class SetPotionLootModifier(val id: Key) : LootModifier {
    override fun type(): Key = LootModifierKeys.SET_POTION
}

internal class SetPotionLootModifierType : LootModifierType {
    override fun codec(
        modifierCodec: Codec<LootModifier>,
        predicateCodec: Codec<ContextPredicate>,
        intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ) = StructCodec.struct(
        "id", Codec.KEY, { (it as SetPotionLootModifier).id },
        ::SetPotionLootModifier,
    )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as SetPotionLootModifier
        val potion = PotionType.fromKey(declaration.id)
            ?: error("No potion type registered for ${declaration.id}")
        return item.with(DataComponents.POTION_CONTENTS, PotionContents(potion))
    }
}
