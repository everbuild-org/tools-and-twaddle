package org.everbuild.twaddle.inventory_transfer

import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.item.ItemStack

enum class TransferAction {
    SHIFT_CLICK
}

enum class TransferRegion {
    OPEN_INVENTORY,
    PLAYER_MAIN,
    PLAYER_HOTBAR,
}

data class TransferContext(
    val action: TransferAction,
    val player: Player,
    val inventory: Inventory,
    val sourceSlot: Int,
    val item: ItemStack,
    val region: TransferRegion,
)