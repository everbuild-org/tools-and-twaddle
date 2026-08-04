package org.everbuild.twaddle.inventory_transfer

@DslMarker
annotation class TransferRulesetDslMarker

@TransferRulesetDslMarker
class TransferRulesetDsl {
    private val rules = mutableListOf<TransferRule>()
    val always: TransferContext.() -> Boolean = { true }

    fun addRule(rule: TransferRule) {
        rules += rule
    }

    fun routeTo(slots: SlotSelector, onlyIf: TransferContext.() -> Boolean = always) {
        rules += TransferRule.RouteTo(slots, onlyIf)
    }

    fun routeTo(slot: Int, onlyIf: TransferContext.() -> Boolean = always) = routeTo(
        slots = SlotSelector.SingleSlot(slot),
        onlyIf = onlyIf
    )

    fun routeTo(progression: IntProgression, onlyIf: TransferContext.() -> Boolean = always) = routeTo(
        slots = SlotSelector.SlotProgression(progression),
        onlyIf = onlyIf
    )

    fun routeTo(slots: List<Int>, onlyIf: TransferContext.() -> Boolean = always) = routeTo(
        slots = SlotSelector.SlotList(slots.toIntArray()),
        onlyIf = onlyIf
    )

    fun blockAt(slots: SlotSelector, onlyIf: TransferContext.() -> Boolean = always) {
        rules += TransferRule.BlockAt(slots, onlyIf)
    }

    fun blockAt(slot: Int, onlyIf: TransferContext.() -> Boolean = always) = blockAt(
        slots = SlotSelector.SingleSlot(slot),
        onlyIf = onlyIf
    )

    fun blockAt(progression: IntProgression, onlyIf: TransferContext.() -> Boolean = always) = blockAt(
        slots = SlotSelector.SlotProgression(progression),
        onlyIf = onlyIf
    )

    fun blockAt(slots: List<Int>, onlyIf: TransferContext.() -> Boolean = always) = blockAt(
        slots = SlotSelector.SlotList(slots.toIntArray()),
        onlyIf = onlyIf
    )

    /**
     * Adds the generic vanilla container routing as a fallback.
     *
    * Declare feature-specific routes and blocks before this call so they can claim their slots first.
     */
    fun vanillaBehaviour() {
        rules.addAll(vanillaBehaviourRules)
    }

    internal fun build(): TransferRuleset = TransferRuleset(rules.toList())
}

fun transferRuleset(block: TransferRulesetDsl.() -> Unit) = TransferRulesetDsl().apply(block).build()
