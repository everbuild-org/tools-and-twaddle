package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
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

data class EnchantRandomlyLootModifier(
    val options: String? = null,
    val onlyCompatible: Boolean = true,
    val includeAdditionalCostComponent: Boolean = false,
) : LootModifier {
    override fun type() = LootModifierKeys.ENCHANT_RANDOMLY
}

internal class EnchantRandomlyLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct(
            "options", Codec.STRING.optional(), { (it as EnchantRandomlyLootModifier).options },
            "only_compatible", Codec.BOOLEAN.optional(), { (it as EnchantRandomlyLootModifier).onlyCompatible.takeIf { compatible -> !compatible } },
            "include_additional_cost_component", Codec.BOOLEAN.optional(), { (it as EnchantRandomlyLootModifier).includeAdditionalCostComponent.takeIf { include -> include } },
            { options, onlyCompatible, includeAdditionalCostComponent ->
                EnchantRandomlyLootModifier(options, onlyCompatible ?: true, includeAdditionalCostComponent ?: false)
            },
        )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as EnchantRandomlyLootModifier
        val registry = context.enchantmentRegistry()
        val options = declaration.options?.let { registry.enchantmentOptions(it) } ?: registry.keys().toList()
        val isBook = item.material() == Material.BOOK
        val candidates = options.filter { key ->
            val enchantment = checkNotNull(registry.get(key))
            !declaration.onlyCompatible || isBook || enchantment.supportedItems().contains(RegistryKey.of(item.material().key()))
        }
        if (candidates.isEmpty()) return item

        val selected = candidates[random.nextInt(candidates.size)]
        val enchantment = checkNotNull(registry.get(selected))
        val level = random.nextInt(enchantment.maxLevel()) + 1
        val result = if (isBook) ItemStack.of(Material.ENCHANTED_BOOK) else item
        val component = if (isBook) DataComponents.STORED_ENCHANTMENTS else DataComponents.ENCHANTMENTS
        val existing = result.get(component) ?: EnchantmentList.EMPTY
        val enchanted = result.with(component, existing.with(selected, level))
        if (declaration.includeAdditionalCostComponent && context.lootAdditionalTradeCostAllowed()) {
            val additionalCost = 2 + random.nextInt(5 + level * 10) + 3 * level
            return enchanted.with(DataComponents.ADDITIONAL_TRADE_COST, additionalCost)
        }
        return enchanted
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
}
