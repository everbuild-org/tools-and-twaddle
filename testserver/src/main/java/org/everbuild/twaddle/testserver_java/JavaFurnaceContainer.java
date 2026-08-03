package org.everbuild.twaddle.testserver_java;

import net.minestom.server.entity.Player;
import net.minestom.server.inventory.Inventory;
import net.minestom.server.inventory.InventoryType;
import net.minestom.server.item.Material;
import org.everbuild.trwaddle.inventory_transfer.InventoryTransferHandler;
import org.everbuild.trwaddle.inventory_transfer.TransferRegion;
import org.everbuild.trwaddle.inventory_transfer.TransferRuleset;
import org.jspecify.annotations.NonNull;

public class JavaFurnaceContainer extends Inventory {
    private static final int SLOT_INPUT = 0;
    private static final int SLOT_FUEL = 1;
    private static final int SLOT_OUTPUT = 2;

    private static final TransferRuleset RULESET = TransferRuleset.builder()
            .blockAt(SLOT_OUTPUT, transfer -> transfer.getRegion() != TransferRegion.OPEN_INVENTORY)
            .routeTo(SLOT_FUEL, transfer -> transfer.getRegion() != TransferRegion.OPEN_INVENTORY
                    && transfer.getItem().material() != Material.COAL)
            .blockAt(SLOT_FUEL, transfer -> transfer.getRegion() != TransferRegion.OPEN_INVENTORY)
            .blockAt(SLOT_INPUT, transfer -> transfer.getRegion() != TransferRegion.OPEN_INVENTORY)
            .vanillaBehaviour()
            .build();

    private final InventoryTransferHandler transferHandler = RULESET.bindTo(this);

    public JavaFurnaceContainer() {
        super(InventoryType.FURNACE, "Furnace");
    }

    @Override
    public boolean shiftClick(@NonNull Player player, int slot, int button) {
        return transferHandler.shiftClick(player, slot, button);
    }
}
