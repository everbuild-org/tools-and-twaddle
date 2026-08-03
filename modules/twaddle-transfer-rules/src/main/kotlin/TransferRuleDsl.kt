package org.everbuild.trwaddle.inventory_transfer

@DslMarker
annotation class TransferRulesetDslMarker

@TransferRulesetDslMarker
class TransferRulesetDsl {
    private val rules = mutableListOf<TransferRule>()

    fun addRule(rule: TransferRule) {
        rules += rule
    }

    fun routeTo(slots: SlotSelector, onlyIf: TransferContext.() -> Boolean) {
        rules += TransferRule.RouteTo(slots, onlyIf)
    }

    fun routeTo(slot: Int, onlyIf: TransferContext.() -> Boolean) = routeTo(
        slots = SlotSelector.SingleSlot(slot),
        onlyIf = onlyIf
    )

    fun routeTo(progression: IntProgression, onlyIf: TransferContext.() -> Boolean) = routeTo(
        slots = SlotSelector.SlotProgression(progression),
        onlyIf = onlyIf
    )

    fun routeTo(slots: List<Int>, onlyIf: TransferContext.() -> Boolean) = routeTo(
        slots = SlotSelector.SlotList(slots.toIntArray()),
        onlyIf = onlyIf
    )

    fun blockAt(slots: SlotSelector, onlyIf: TransferContext.() -> Boolean) {
        rules += TransferRule.BlockAt(slots, onlyIf)
    }

    fun blockAt(slot: Int, onlyIf: TransferContext.() -> Boolean) = blockAt(
        slots = SlotSelector.SingleSlot(slot),
        onlyIf = onlyIf
    )

    fun blockAt(progression: IntProgression, onlyIf: TransferContext.() -> Boolean) = blockAt(
        slots = SlotSelector.SlotProgression(progression),
        onlyIf = onlyIf
    )

    fun blockAt(slots: List<Int>, onlyIf: TransferContext.() -> Boolean) = blockAt(
        slots = SlotSelector.SlotList(slots.toIntArray()),
        onlyIf = onlyIf
    )

    internal fun build(): TransferRuleset = TransferRuleset(rules.toList())
}

fun transferRuleset(block: TransferRulesetDsl.() -> Unit) = TransferRulesetDsl().apply(block).build()