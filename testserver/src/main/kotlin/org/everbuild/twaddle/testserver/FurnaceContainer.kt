package org.everbuild.twaddle.testserver

import net.minestom.server.entity.Player
import net.minestom.server.inventory.Inventory
import net.minestom.server.inventory.InventoryType
import net.minestom.server.item.Material
import org.everbuild.twaddle.inventory_transfer.TransferRegion
import org.everbuild.twaddle.inventory_transfer.transferRuleset

class FurnaceContainer : Inventory(InventoryType.FURNACE, "Furnace") {
    private val transfers = ruleset.bindTo(this)

    override fun shiftClick(player: Player, slot: Int, button: Int): Boolean =
        transfers.shiftClick(player, slot, button)

    companion object {
        const val SLOT_INPUT = 0
        const val SLOT_FUEL = 1
        const val SLOT_OUTPUT = 2

        val ruleset = transferRuleset {
            blockAt(SLOT_OUTPUT) { region != TransferRegion.OPEN_INVENTORY }

            routeTo(SLOT_FUEL) { region != TransferRegion.OPEN_INVENTORY && item.material() == Material.COAL }
            blockAt(SLOT_FUEL) { region != TransferRegion.OPEN_INVENTORY }

            routeTo(SLOT_INPUT) { region != TransferRegion.OPEN_INVENTORY }

            vanillaBehaviour()
        }
    }
}
