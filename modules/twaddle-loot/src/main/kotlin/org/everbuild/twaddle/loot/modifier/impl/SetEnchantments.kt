package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.loot.modifier.context.enchantmentRegistry

data class SetEnchantmentsLootModifier(
    val enchantments: Map<Key, NumberProviderDeclaration<Int>> = emptyMap(),
    val add: Boolean = false,
) : LootModifier {
    override fun type() = LootModifierKeys.SET_ENCHANTMENTS
}

internal class SetEnchantmentsLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct(
            "enchantments", Codec.KEY.mapValue(intNumberProviderCodec).optional(emptyMap()), { (it as SetEnchantmentsLootModifier).enchantments },
            "add", Codec.BOOLEAN.optional(), { (it as SetEnchantmentsLootModifier).add.takeIf { add -> add } },
            { enchantments, add -> SetEnchantmentsLootModifier(enchantments, add ?: false) },
        )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as SetEnchantmentsLootModifier
        val enchantmentRegistry = context.enchantmentRegistry()
        val result = if (item.material() == Material.BOOK) item.withMaterial(Material.ENCHANTED_BOOK) else item
        val component = if (result.material() == Material.ENCHANTED_BOOK) {
            DataComponents.STORED_ENCHANTMENTS
        } else {
            DataComponents.ENCHANTMENTS
        }
        val current = result.get(component) ?: EnchantmentList.EMPTY
        val updated = declaration.enchantments.entries.fold(current) { enchantments, (key, levelProvider) ->
            val enchantment = RegistryKey.of<Enchantment>(key)
            check(enchantmentRegistry.get(enchantment) != null) { "No enchantment registered for $key" }
            val level = levelProvider.get(context, random) + if (declaration.add) enchantments.level(enchantment) else 0
            val clampedLevel = level.coerceIn(0, 255)
            if (clampedLevel == 0) enchantments.remove(enchantment) else enchantments.with(enchantment, clampedLevel)
        }
        return result.with(component, updated)
    }

}
