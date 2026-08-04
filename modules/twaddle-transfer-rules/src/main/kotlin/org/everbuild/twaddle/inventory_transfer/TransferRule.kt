package org.everbuild.twaddle.inventory_transfer

/**
 * Rules are evaluated from first to last.
 *
 * For each destination slot, the first applicable rule containing that slot
 * decides whether it is routed to or blocked. Later rules cannot override that
 * decision.
 *
 * Each applicable [TransferRule.RouteTo] forms an ordered routing tier.
 */
sealed interface TransferRule {
    val slots: SlotSelector
    val onlyIf: TransferPredicate

    data class RouteTo(
        override val slots: SlotSelector,
        override val onlyIf: TransferPredicate,
    ) : TransferRule

    data class BlockAt(
        override val slots: SlotSelector,
        override val onlyIf: TransferPredicate,
    ) : TransferRule
}