package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.EnchantmentLevelContext
import kotlin.math.max

data class ApplyBonusParameters(
    val extra: Int? = null,
    val probability: Float? = null,
    val bonusMultiplier: Int? = null,
)

data class ApplyBonusLootModifier(
    val enchantment: Key,
    val formula: String,
    val parameters: ApplyBonusParameters? = null,
) : LootModifier {
    override fun type() = LootModifierKeys.APPLY_BONUS
}

internal class ApplyBonusLootModifierType : LootModifierType {
    private val parametersCodec = StructCodec.struct(
        "extra", Codec.INT.optional(), ApplyBonusParameters::extra,
        "probability", Codec.FLOAT.optional(), ApplyBonusParameters::probability,
        "bonusMultiplier", Codec.INT.optional(), ApplyBonusParameters::bonusMultiplier,
        ::ApplyBonusParameters,
    )

    override fun codec(
        modifierCodec: Codec<LootModifier>,
        predicateCodec: Codec<ContextPredicate>,
        intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ) = StructCodec.struct(
        "enchantment", Codec.KEY, { (it as ApplyBonusLootModifier).enchantment },
        "formula", Codec.STRING, { (it as ApplyBonusLootModifier).formula },
        "parameters", parametersCodec.optional(), { (it as ApplyBonusLootModifier).parameters },
        ::ApplyBonusLootModifier,
    )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as ApplyBonusLootModifier
        val level = context[EnchantmentLevelContext]?.enchantments()?.entries
            ?.firstOrNull { it.key.key() == declaration.enchantment }?.value ?: 0
        if (level <= 0 || item.amount() <= 0) return item

        val bonus = when (declaration.formula) {
            "minecraft:binomial_with_bonus_count" -> {
                val parameters = requireNotNull(declaration.parameters) {
                    "binomial_with_bonus_count requires parameters"
                }
                val trials = Math.addExact(
                    level,
                    requireNotNull(parameters.extra) { "binomial_with_bonus_count requires parameters.extra" },
                )
                val probability = requireNotNull(parameters.probability) {
                    "binomial_with_bonus_count requires parameters.probability"
                }
                require(trials >= 0) { "binomial_with_bonus_count trials must be non-negative" }
                require(probability in 0f..1f) { "binomial_with_bonus_count probability must be between 0 and 1" }
                (0 until trials).count { random.nextFloat() < probability }
            }

            "minecraft:uniform_bonus_count" -> {
                val multiplier = requireNotNull(declaration.parameters?.bonusMultiplier) {
                    "uniform_bonus_count requires parameters.bonusMultiplier"
                }
                require(multiplier >= 0) { "uniform_bonus_count bonusMultiplier must be non-negative" }
                random.nextInt(Math.addExact(Math.multiplyExact(level, multiplier), 1))
            }

            "minecraft:ore_drops" -> {
                val multiplier = max(1, random.nextInt(level + 2))
                return item.withAmount(Math.multiplyExact(item.amount(), multiplier))
            }

            else -> error("Unsupported apply_bonus formula '${declaration.formula}'")
        }

        return item.withAmount(Math.addExact(item.amount(), bonus))
    }
}
