package org.everbuild.twaddle.datapack_primitives.slot_source

import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext

/** A data-driven source of item stacks selected from a loot-context inventory. */
interface SlotSource {
    fun type(): Key

    /** Returns one stack per selected slot, in source order; empty slots are represented by [ItemStack.AIR]. */
    fun evaluate(context: DataContext): List<ItemStack>
}
