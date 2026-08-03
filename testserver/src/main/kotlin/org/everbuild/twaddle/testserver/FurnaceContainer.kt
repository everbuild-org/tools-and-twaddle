package org.everbuild.twaddle.testserver

import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.Material
import org.everbuild.trwaddle.inventory_transfer.activeTransferRuleset
import org.everbuild.trwaddle.inventory_transfer.transferRuleset

class FurnaceContainer : Inventory(InventoryType.FURNACE, "Furnace") {
    init {
        this.activeTransferRuleset = transferRuleset {
            blockAt(SLOT_OUTPUT) { true }
            blockAt(SLOT_FUEL) { item.material() != Material.COAL }
            routeTo(SLOT_FUEL) { item.material() == Material.COAL }
        }
    }

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_FUEL = 1
        const val SLOT_OUTPUT = 2
    }
}