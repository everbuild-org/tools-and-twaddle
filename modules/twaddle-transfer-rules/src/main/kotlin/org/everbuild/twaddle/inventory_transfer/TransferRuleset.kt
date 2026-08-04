package org.everbuild.twaddle.inventory_transfer

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
                .slotsInOrder(context)
                .filter(claimedSlots::add)
                .toList()

            if (rule is TransferRule.RouteTo && unclaimedSlots.isNotEmpty()) {
                routingTiers += unclaimedSlots
            }
        }

        return routingTiers
    }

    fun bindTo(inventory: Inventory): InventoryTransferHandler =
        InventoryTransferHandler.bind(inventory, this)

    class Builder {
        private val rules = mutableListOf<TransferRule>()

        fun addRule(rule: TransferRule): Builder {
            rules += rule
            return this
        }

        @JvmOverloads
        fun routeTo(slots: SlotSelector, onlyIf: TransferPredicate = TransferPredicate.ALWAYS): Builder {
            rules += TransferRule.RouteTo(slots, onlyIf)
            return this
        }

        @JvmOverloads
        fun routeTo(slot: Int, onlyIf: TransferPredicate = TransferPredicate.ALWAYS) = routeTo(
            slots = SlotSelector.SingleSlot(slot),
            onlyIf = onlyIf
        )

        @JvmOverloads
        fun blockAt(slots: SlotSelector, onlyIf: TransferPredicate = TransferPredicate.ALWAYS): Builder {
            rules += TransferRule.BlockAt(slots, onlyIf)
            return this
        }

        @JvmOverloads
        fun blockAt(slot: Int, onlyIf: TransferPredicate = TransferPredicate.ALWAYS) = blockAt(
            slots = SlotSelector.SingleSlot(slot),
            onlyIf = onlyIf
        )

        fun vanillaBehaviour(): Builder {
            rules.addAll(vanillaBehaviourRules)
            return this
        }

        fun build(): TransferRuleset = TransferRuleset(rules.toList())
    }

    companion object {
        @JvmStatic
        fun builder(): Builder = Builder()
    }
}

private fun SlotSelector.slotsInOrder(context: TransferContext): Sequence<Int> = when (this) {
    is SlotSelector.SingleSlot -> sequenceOf(slot)
    is SlotSelector.SlotRange -> when (order) {
        ChooseSlotOrder.FIRST_TO_LAST -> (startInclusive..endInclusive).asSequence()
        ChooseSlotOrder.LAST_TO_FIRST -> (endInclusive downTo startInclusive).asSequence()
    }
    is SlotSelector.SlotProgression -> progression.asSequence()
    is SlotSelector.SlotList -> slots.asSequence()
    is ContextualSlotSelector -> select(context).asSequence()
}
