package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import org.everbuild.twaddle.datapack_primitives.context.DataContext

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class SetCountLootModifier(
    val count: NumberProviderDeclaration<Int>,
    val add: Boolean = false,
) : LootModifier {
    override fun type(): Key = LootModifierKeys.SET_COUNT
}

internal class SetCountLootModifierType : LootModifierType {
    override fun codec(
        modifierCodec: Codec<LootModifier>,
        predicateCodec: Codec<ContextPredicate>,
        intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ) = StructCodec.struct(
        "count", intNumberProviderCodec, { (it as SetCountLootModifier).count },
        "add", Codec.BOOLEAN.optional(), { (it as SetCountLootModifier).add.takeIf { add -> add } },
        { count, add -> SetCountLootModifier(count, add ?: false) },
    )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as SetCountLootModifier
        val count = declaration.count.get(context, random) + if (declaration.add) item.amount() else 0
        return if (count <= 0) ItemStack.AIR else item.withAmount(count)
    }
}
