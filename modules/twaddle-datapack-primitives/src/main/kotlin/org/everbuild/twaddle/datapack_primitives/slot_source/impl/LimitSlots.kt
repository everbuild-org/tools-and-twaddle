package org.everbuild.twaddle.datapack_primitives.slot_source.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceKeys
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceType

private data class LimitSlotsSlotSource(val limit: Int, val slotSource: SlotSource) : SlotSource {
    init { require(limit >= 0) { "Slot source limit must be nonnegative" } }
    override fun type() = SlotSourceKeys.LIMIT_SLOTS
    override fun evaluate(context: DataContext): List<ItemStack> = slotSource.evaluate(context).take(limit)
}

internal data object LimitSlotsSlotSourceType : SlotSourceType {
    override fun codec(slotSourceCodec: Codec<SlotSource>): StructCodec<out SlotSource> = StructCodec.struct(
        "limit", Codec.INT, { (it as LimitSlotsSlotSource).limit },
        "slot_source", slotSourceCodec, { (it as LimitSlotsSlotSource).slotSource },
        ::LimitSlotsSlotSource,
    )
}
