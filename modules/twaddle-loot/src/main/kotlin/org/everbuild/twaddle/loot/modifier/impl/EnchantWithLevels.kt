package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import kotlin.math.roundToInt
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import net.minestom.server.registry.TagKey
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.loot.modifier.context.enchantmentRegistry
import org.everbuild.twaddle.loot.modifier.context.lootAdditionalTradeCostAllowed
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class EnchantWithLevelsLootModifier(
    val levels: NumberProviderDeclaration<Int>,
    val options: String? = null,
    val includeAdditionalCostComponent: Boolean = false,
) : LootModifier {
    override fun type() = LootModifierKeys.ENCHANT_WITH_LEVELS
}

internal class EnchantWithLevelsLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct(
            "levels", intNumberProviderCodec, { (it as EnchantWithLevelsLootModifier).levels },
            "options", Codec.STRING.optional(), { (it as EnchantWithLevelsLootModifier).options },
            "include_additional_cost_component", Codec.BOOLEAN.optional(), { (it as EnchantWithLevelsLootModifier).includeAdditionalCostComponent.takeIf { include -> include } },
            { levels, options, includeAdditionalCostComponent ->
                EnchantWithLevelsLootModifier(levels, options, includeAdditionalCostComponent ?: false)
            },
        )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as EnchantWithLevelsLootModifier
        val enchantability = item.get(DataComponents.ENCHANTABLE) ?: return item
        if (enchantability <= 0) return item

        val registry = context.enchantmentRegistry()
        val options = declaration.options?.let { registry.enchantmentOptions(it) } ?: registry.keys().toList()
        val isBook = item.material() == Material.BOOK
        val baseLevelCost = declaration.levels.get(context, random)
        var levelCost = baseLevelCost
        levelCost += 1 + random.nextInt(enchantability / 4 + 1) + random.nextInt(enchantability / 4 + 1)
        val randomSpan = (random.nextFloat() + random.nextFloat() - 1f) * 0.15f
        levelCost = (levelCost + levelCost * randomSpan).roundToInt().coerceAtLeast(1)
        val available = options.mapNotNull { key ->
            val enchantment = registry.get(key) ?: error("No enchantment registered for $key")
            val itemKey = RegistryKey.of<Material>(item.material().key())
            val applicable = isBook || (enchantment.primaryItems()?.contains(itemKey) ?: enchantment.supportedItems().contains(itemKey))
            if (!applicable) return@mapNotNull null

            val level = (enchantment.maxLevel() downTo 1).firstOrNull { candidateLevel ->
                levelCost >= enchantment.minCost().base() + enchantment.minCost().perLevelAboveFirst() * (candidateLevel - 1) &&
                    levelCost <= enchantment.maxCost().base() + enchantment.maxCost().perLevelAboveFirst() * (candidateLevel - 1)
            } ?: return@mapNotNull null
            AvailableEnchantment(key, level, enchantment.weight())
        }.toMutableList()

        val selected = mutableListOf<AvailableEnchantment>()
        if (available.isNotEmpty()) {
            selectWeighted(available, random)?.let(selected::add)
            while (random.nextInt(50) <= levelCost) {
                val previous = selected.lastOrNull()
                if (previous != null) {
                    available.removeAll { candidate -> !registry.enchantmentsCompatible(previous.key, candidate.key) }
                }
                if (available.isEmpty()) break
                selectWeighted(available, random)?.let(selected::add)
                levelCost /= 2
            }
        }

        val result = if (isBook) ItemStack.of(Material.ENCHANTED_BOOK) else item
        val component = if (isBook) DataComponents.STORED_ENCHANTMENTS else DataComponents.ENCHANTMENTS
        val existing = result.get(component) ?: EnchantmentList.EMPTY
        val enchanted = selected.fold(result) { stack, enchantment ->
            val current = stack.get(component) ?: existing
            stack.with(component, current.with(enchantment.key, enchantment.level))
        }
        if (declaration.includeAdditionalCostComponent && context.lootAdditionalTradeCostAllowed() && baseLevelCost > 0) {
            return enchanted.with(DataComponents.ADDITIONAL_TRADE_COST, baseLevelCost)
        }
        return enchanted
    }

    private data class AvailableEnchantment(val key: RegistryKey<Enchantment>, val level: Int, val weight: Int)

    private fun selectWeighted(
        candidates: List<AvailableEnchantment>,
        random: Random,
    ): AvailableEnchantment? {
        val totalWeight = candidates.sumOf { it.weight }
        if (totalWeight <= 0) return null
        var selected = random.nextInt(totalWeight)
        for (candidate in candidates) {
            selected -= candidate.weight
            if (selected < 0) return candidate
        }
        return null
    }

    private fun net.minestom.server.registry.DynamicRegistry<Enchantment>.enchantmentOptions(option: String): List<RegistryKey<Enchantment>> {
        if (option.startsWith("#")) {
            val tag = getTag(TagKey.ofHash(option)) ?: error("No enchantment tag registered for $option")
            return tag.toList()
        }
        val key = RegistryKey.of<Enchantment>(net.kyori.adventure.key.Key.key(option))
        check(get(key) != null) { "No enchantment registered for $option" }
        return listOf(key)
    }

    private fun net.minestom.server.registry.DynamicRegistry<Enchantment>.enchantmentsCompatible(
        left: RegistryKey<Enchantment>,
        right: RegistryKey<Enchantment>,
    ): Boolean {
        if (left == right) return false
        val leftEnchantment = get(left) ?: return false
        val rightEnchantment = get(right) ?: return false
        return !leftEnchantment.exclusiveSet().contains(right) && !rightEnchantment.exclusiveSet().contains(left)
    }
}
