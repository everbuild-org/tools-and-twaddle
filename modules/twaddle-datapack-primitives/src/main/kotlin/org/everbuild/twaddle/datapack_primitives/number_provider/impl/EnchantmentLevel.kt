package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderCondition
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderKeys
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.context.EnchantmentLevelContext
import org.everbuild.twaddle.datapack_primitives.number_provider.context.NumberProviderContext
import java.util.Random

private data class LinearEnchantmentAmount(
    val type: String,
    val base: Float,
    val perLevelAboveFirst: Float,
) {
    fun at(level: Int): Float = base + (level - 1).coerceAtLeast(0) * perLevelAboveFirst
}

private data class EnchantmentLevelDeclaration(
    val amount: LinearEnchantmentAmount,
) : NumberProviderDeclaration<Float> {
    override fun type() = NumberProviderKeys.ENCHANTMENT_LEVEL
    override fun get(context: NumberProviderContext, random: Random): Float {
        val levels = context.get(EnchantmentLevelContext)?.enchantments()?.values?.toList().orEmpty()
        val level = if (levels.isEmpty()) 0 else levels[random.nextInt(levels.size)]
        return amount.at(level)
    }
}

internal class EnchantmentLevelNumberProviderType : NumberProviderType<Float> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<Float>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<NumberProviderCondition>,
        arithmetic: NumberProviderArithmetic<Float>,
    ): StructCodec<NumberProviderDeclaration<Float>> {
        val amountCodec = StructCodec.struct(
            "type", Codec.STRING,
            { (it as LinearEnchantmentAmount).type },
            "base", Codec.FLOAT,
            { (it as LinearEnchantmentAmount).base },
            "per_level_above_first", Codec.FLOAT,
            { (it as LinearEnchantmentAmount).perLevelAboveFirst },
            { type, base, perLevel ->
                require(type == "minecraft:linear") { "Unsupported enchantment amount type: $type" }
                LinearEnchantmentAmount(type, base, perLevel)
            },
        )
        return StructCodec.struct(
            "amount", amountCodec,
            { (it as EnchantmentLevelDeclaration).amount },
            { amount -> EnchantmentLevelDeclaration(amount) },
        ).asProviderCodec()
    }
}
