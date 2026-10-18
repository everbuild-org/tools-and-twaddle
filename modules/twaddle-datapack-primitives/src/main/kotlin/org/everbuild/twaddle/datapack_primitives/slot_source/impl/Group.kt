package org.everbuild.twaddle.datapack_primitives.slot_source.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceKeys
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceType

internal data class GroupSlotSource(val sources: List<SlotSource>) : SlotSource {
    override fun type() = SlotSourceKeys.GROUP
    override fun evaluate(context: DataContext): List<ItemStack> = sources.flatMap { it.evaluate(context) }
}

internal data object GroupSlotSourceType : SlotSourceType {
    override fun codec(slotSourceCodec: Codec<SlotSource>): StructCodec<out SlotSource> = StructCodec.struct(
        "sources", slotSourceCodec.list(), { (it as GroupSlotSource).sources },
        ::GroupSlotSource,
    )
}
