package org.everbuild.trwaddle.inventory_transfer

import net.minestom.server.inventory.Inventory

class TransferRuleset(
    private val rules: List<TransferRule>,
) {
    fun resolve(context: TransferContext): List<List<Int>> {
        val claimedSlots = mutableSetOf<Int>()
        val routingTiers = mutableListOf<List<Int>>()

        rules.forEach { rule ->
            if (!rule.onlyIf.invoke(context)) return@forEach

            val unclaimedSlots = rule.slots
                .slotsInOrder()
                .filter(claimedSlots::add)
                .toList()

            if (rule is TransferRule.RouteTo && unclaimedSlots.isNotEmpty()) {
                routingTiers += unclaimedSlots
            }
        }

        return routingTiers
    }

    @JvmName("install")
    fun javaInstall(inventory: Inventory) {
        inventory.activeTransferRuleset = this
    }

    class Builder {
        private val rules = mutableListOf<TransferRule>()

        fun addRule(rule: TransferRule) {
            rules += rule
        }

        @JvmOverloads
        fun routeTo(slots: SlotSelector, onlyIf: TransferPredicate = TransferPredicate.ALWAYS) {
            rules += TransferRule.RouteTo(slots, onlyIf)
        }

        @JvmOverloads
        fun routeTo(slot: Int, onlyIf: TransferPredicate = TransferPredicate.ALWAYS) = routeTo(
            slots = SlotSelector.SingleSlot(slot),
            onlyIf = onlyIf
        )

        @JvmOverloads
        fun blockAt(slots: SlotSelector, onlyIf: TransferPredicate = TransferPredicate.ALWAYS) {
            rules += TransferRule.BlockAt(slots, onlyIf)
        }

        @JvmOverloads
        fun blockAt(slot: Int, onlyIf: TransferPredicate = TransferPredicate.ALWAYS) = blockAt(
            slots = SlotSelector.SingleSlot(slot),
            onlyIf = onlyIf
        )

        fun build(): TransferRuleset = TransferRuleset(rules.toList())
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}

private fun SlotSelector.slotsInOrder(): Sequence<Int> = when (this) {
    is SlotSelector.SingleSlot -> sequenceOf(slot)
    is SlotSelector.SlotRange -> when (order) {
        ChooseSlotOrder.FIRST_TO_LAST -> (startInclusive..endInclusive).asSequence()
        ChooseSlotOrder.LAST_TO_FIRST -> (endInclusive downTo startInclusive).asSequence()
    }
    is SlotSelector.SlotProgression -> progression.asSequence()
    is SlotSelector.SlotList -> slots.asSequence()
}
