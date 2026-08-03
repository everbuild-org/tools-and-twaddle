package org.everbuild.trwaddle.inventory_transfer

import net.minestom.server.inventory.Inventory
import net.minestom.server.tag.Tag

private val rulesetTag = Tag.Transient<TransferRuleset>("twaddle-transfer-ruleset")
private val handlerTag = Tag.Transient<Boolean>("twaddle-transfer-event-handler")

private fun installHandlers(inventory: Inventory) {
    TODO("implement applying the ruleset handler here that'll read from the inventory's tag")
}

var Inventory.activeTransferRuleset: TransferRuleset?
    get() = this.getTag(rulesetTag)
    set(value) {
        if (value != null && getTag(handlerTag) == null) {
            installHandlers(this)
            setTag(handlerTag, true)
        }

        setTag(rulesetTag, value)
    }