package org.everbuild.twaddle.inventory_transfer

import net.minestom.server.item.ItemStack

internal data class TransferResult(
    val remainder: ItemStack,
    val changes: Map<Int, ItemStack>,
) {
    val moved: Boolean
        get() = changes.isNotEmpty()
}

internal object TransferProcessor {
    fun plan(
        item: ItemStack,
        targetItems: List<ItemStack>,
        destinationTiers: List<List<Int>>,
    ): TransferResult {
        if (item.isAir) return TransferResult(ItemStack.AIR, emptyMap())

        val targetState = targetItems.toMutableList()
        val changes = linkedMapOf<Int, ItemStack>()
        var remainder = item

        for (tier in destinationTiers) {
            for (slot in tier) {
                if (slot !in targetState.indices) continue

                val target = targetState[slot]
                if (target.isAir || !remainder.isSimilar(target)) continue

                val available = target.maxStackSize() - target.amount()
                if (available <= 0) continue

                val movedAmount = minOf(remainder.amount(), available)
                val updatedTarget = target.withAmount(target.amount() + movedAmount)
                targetState[slot] = updatedTarget
                changes[slot] = updatedTarget
                remainder = remainder.afterMoving(movedAmount)
                if (remainder.isAir) break
            }

            if (remainder.isAir) break

            for (slot in tier) {
                if (slot !in targetState.indices || !targetState[slot].isAir) continue

                val movedAmount = minOf(remainder.amount(), remainder.maxStackSize())
                val updatedTarget = remainder.withAmount(movedAmount)
                targetState[slot] = updatedTarget
                changes[slot] = updatedTarget
                remainder = remainder.afterMoving(movedAmount)
                if (remainder.isAir) break
            }

            if (remainder.isAir) break
        }

        return TransferResult(remainder, changes.toMap())
    }
}

private fun ItemStack.afterMoving(amount: Int): ItemStack {
    val remainingAmount = amount() - amount
    return if (remainingAmount == 0) ItemStack.AIR else withAmount(remainingAmount)
}
