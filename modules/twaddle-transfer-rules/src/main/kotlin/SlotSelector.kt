package org.everbuild.trwaddle.inventory_transfer

/**
 * Represents a strategy for selecting specific slots within an inventory.
 */
sealed interface SlotSelector {
    data class SingleSlot(val slot: Int) : SlotSelector

    /**
     * Only for use from Java, use [SlotProgression] in kotlin.
     */
    data class SlotRange(val startInclusive: Int, val endInclusive: Int, val order: ChooseSlotOrder) : SlotSelector {
        init {
            require(startInclusive <= endInclusive) { "Start must be less than or equal to end" }
        }
    }

    data class SlotProgression(val progression: IntProgression) : SlotSelector
    data class SlotList(val slots: IntArray) : SlotSelector {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as SlotList

            return slots.contentEquals(other.slots)
        }

        override fun hashCode(): Int {
            return slots.contentHashCode()
        }
    }

    companion object {
        @JvmStatic
        fun single(slot: Int) = SingleSlot(slot)

        @JvmStatic
        fun range(startInclusive: Int, endInclusive: Int, order: ChooseSlotOrder = ChooseSlotOrder.FIRST_TO_LAST) =
            SlotRange(startInclusive, endInclusive, order)

        @JvmStatic
        fun progression(progression: IntProgression) = SlotProgression(progression)

        @JvmStatic
        fun list(slots: IntArray) = SlotList(slots)
    }
}

internal class ContextualSlotSelector(
    val select: (TransferContext) -> IntProgression,
) : SlotSelector
