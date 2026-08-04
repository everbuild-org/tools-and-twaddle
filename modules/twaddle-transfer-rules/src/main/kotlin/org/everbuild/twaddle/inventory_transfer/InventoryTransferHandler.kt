package org.everbuild.twaddle.inventory_transfer

import net.minestom.server.entity.Player
import net.minestom.server.event.EventDispatcher
import net.minestom.server.event.inventory.InventoryClickEvent
import net.minestom.server.inventory.AbstractInventory
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.click.ClickType

private const val HOTBAR_SIZE = 9

class InventoryTransferHandler internal constructor(
    private val inventory: Inventory,
    private val ruleset: TransferRuleset,
) {
    fun shiftClick(player: Player, slot: Int, @Suppress("unused") button: Int): Boolean {
        val playerInventory = player.getInventory()
        val isInWindow = slot in 0 until inventory.size
        val sourceSlot = if (isInWindow) slot else slot - inventory.size
        val sourceInventory: AbstractInventory = if (isInWindow) inventory else playerInventory
        val targetInventory: AbstractInventory = if (isInWindow) playerInventory else inventory
        val sourceSize = if (isInWindow) inventory.size else playerInventory.innerSize
        if (sourceSlot !in 0 until sourceSize) return reject(player)

        val clicked = sourceInventory.getItemStack(sourceSlot)
        if (clicked.isAir) return reject(player)

        val region = when {
            isInWindow -> TransferRegion.OPEN_INVENTORY
            sourceSlot < HOTBAR_SIZE -> TransferRegion.PLAYER_HOTBAR
            else -> TransferRegion.PLAYER_MAIN
        }

        // vanillaBehaviour() supplies the generic hotbar/main fallback. Equipment, off-hand, and
        // crafting-grid routing can use the same model once PlayerInventory bindings are supported.
        val context = TransferContext(
            action = TransferAction.SHIFT_CLICK,
            player = player,
            inventory = inventory,
            sourceSlot = sourceSlot,
            item = clicked,
            region = region,
        )
        val transfer = TransferProcessor.plan(
            item = clicked,
            targetItems = targetInventory.itemStacks.asList(),
            destinationTiers = ruleset.resolve(context),
        )
        if (!transfer.moved) return reject(player)

        val cursor = playerInventory.cursorItem
        transfer.changes.forEach { (destinationSlot, item) ->
            targetInventory.setItemStack(destinationSlot, item)
            EventDispatcher.call(
                InventoryClickEvent(
                    targetInventory,
                    player,
                    destinationSlot,
                    ClickType.SHIFT_CLICK,
                    item,
                    cursor,
                )
            )
        }
        sourceInventory.setItemStack(sourceSlot, transfer.remainder)
        inventory.updateAll(player)
        return true
    }

    private fun reject(player: Player): Boolean {
        inventory.updateAll(player)
        return false
    }

    companion object {
        @JvmStatic
        fun bind(inventory: Inventory, ruleset: TransferRuleset): InventoryTransferHandler =
            InventoryTransferHandler(inventory, ruleset)
    }
}

private fun Inventory.updateAll(player: Player) {
    player.getInventory().update()
    update(player)
}
