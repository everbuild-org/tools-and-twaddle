package org.everbuild.twaddle.datapack_primitives.slot_source.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.ItemPredicateMatcherContext
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceKeys
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceType

private data class FilteredSlotSource(val itemFilter: Codec.RawValue, val slotSource: SlotSource) : SlotSource {
    override fun type() = SlotSourceKeys.FILTERED
    override fun evaluate(context: DataContext): List<ItemStack> {
        val matcher = context[ItemPredicateMatcherContext]
        return slotSource.evaluate(context).filter { matcher.matches(it, itemFilter) }
    }
}

internal data object FilteredSlotSourceType : SlotSourceType {
    override fun codec(slotSourceCodec: Codec<SlotSource>): StructCodec<out SlotSource> = StructCodec.struct(
        "item_filter", Codec.RAW_VALUE, { (it as FilteredSlotSource).itemFilter },
        "slot_source", slotSourceCodec, { (it as FilteredSlotSource).slotSource },
        ::FilteredSlotSource,
    )
}
