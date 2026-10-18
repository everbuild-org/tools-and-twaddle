package org.everbuild.twaddle.loot.modifier

import net.minestom.server.item.ItemStack
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

/** Applies a registered modifier using the shared datapack context and provider random source. */
fun applyLootModifier(
    modifier: LootModifier,
    item: ItemStack,
    registry: Registry<LootModifierType>,
    context: DataContext,
    random: Random,
): ItemStack? {
    val type = registry.get(modifier.type())
        ?: error("No loot modifier type registered for ${modifier.type()}")
    return type.apply(modifier, item, context, random)
}
