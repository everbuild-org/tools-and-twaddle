package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.EnchantmentLevelContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import kotlin.math.floor

data class EnchantedCountIncreaseLootModifier(
    val count: NumberProviderDeclaration<Float>,
    val enchantment: Key,
    val limit: Int = 0,
) : LootModifier {
    override fun type() = LootModifierKeys.ENCHANTED_COUNT_INCREASE
}

internal class EnchantedCountIncreaseLootModifierType : LootModifierType {
    override fun codec(
        modifierCodec: Codec<LootModifier>,
        predicateCodec: Codec<ContextPredicate>,
        intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ) = StructCodec.struct(
        "count", floatNumberProviderCodec, { (it as EnchantedCountIncreaseLootModifier).count },
        "enchantment", Codec.KEY, { (it as EnchantedCountIncreaseLootModifier).enchantment },
        "limit", Codec.INT.optional(), { (it as EnchantedCountIncreaseLootModifier).limit.takeIf { limit -> limit != 0 } },
        { count, enchantment, limit -> EnchantedCountIncreaseLootModifier(count, enchantment, limit ?: 0) },
    )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as EnchantedCountIncreaseLootModifier
        require(declaration.limit >= 0) { "enchanted_count_increase limit must be non-negative" }
        val level = context[EnchantmentLevelContext]?.enchantments()?.entries
            ?.firstOrNull { it.key.key() == declaration.enchantment }?.value ?: 0
        if (level <= 0) return item

        val extra = floor(declaration.count.get(context, random) * level).toInt()
        val resultingCount = Math.addExact(item.amount(), extra)
        val limitedCount = if (declaration.limit == 0) resultingCount else minOf(resultingCount, declaration.limit)
        return if (limitedCount <= 0) ItemStack.AIR else item.withAmount(limitedCount)
    }
}
