package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.utils.Either
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.EnchantmentLevelContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private fun interface EnchantedChance {
    fun at(level: Int): Float
}

private data class LinearEnchantedChance(val base: Float, val perLevelAboveFirst: Float) : EnchantedChance {
    override fun at(level: Int) = base + (level - 1).coerceAtLeast(0) * perLevelAboveFirst
}

private data class RandomChanceWithEnchantedBonusPredicate(
    val enchantment: Key,
    val unenchantedChance: Float,
    val enchantedChance: EnchantedChance,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.RANDOM_CHANCE_WITH_ENCHANTED_BONUS

    override fun test(context: DataContext, random: Random): Boolean {
        val level = context[EnchantmentLevelContext]?.enchantments()?.entries
            ?.firstOrNull { it.key.key() == enchantment }?.value ?: 0
        val chance = if (level == 0) unenchantedChance else enchantedChance.at(level)
        return random.nextFloat() < chance
    }
}

internal class RandomChanceWithEnchantedBonusPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> {
        val linearCodec = StructCodec.struct(
            "type", Codec.STRING, { "minecraft:linear" },
            "base", Codec.FLOAT, LinearEnchantedChance::base,
            "per_level_above_first", Codec.FLOAT, LinearEnchantedChance::perLevelAboveFirst,
            { type, base, perLevel ->
                require(type == "minecraft:linear") { "Unsupported enchanted chance type: $type" }
                LinearEnchantedChance(base, perLevel)
            },
        )
        val chanceCodec = Codec.Either(Codec.FLOAT, linearCodec).transform(
            { value -> value.unify<EnchantedChance>({ chance -> LinearEnchantedChance(chance, 0f) }, { it }) },
            { chance ->
                when (chance) {
                    is LinearEnchantedChance -> Either.right(chance)
                    else -> error("Unsupported enchanted chance value: $chance")
                }
            },
        )
        return StructCodec.struct(
            "enchantment", Codec.KEY, RandomChanceWithEnchantedBonusPredicate::enchantment,
            "unenchanted_chance", Codec.FLOAT, RandomChanceWithEnchantedBonusPredicate::unenchantedChance,
            "enchanted_chance", chanceCodec, RandomChanceWithEnchantedBonusPredicate::enchantedChance,
            ::RandomChanceWithEnchantedBonusPredicate,
        ).asContextPredicateCodec()
    }
}
