package org.everbuild.twaddle.datapack_primitives.slot_source.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.Either
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.SlotSourceInventoryContext
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceKeys
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceType

private val slotPatternsCodec: Codec<List<String>> = Codec.Either(
    Codec.STRING,
    Codec.STRING.list(),
).transform(
    { encoded -> encoded.unify({ listOf(it) }, { it }) },
    { patterns -> if (patterns.size == 1) Either.left(patterns.single()) else Either.right(patterns) },
)

private val sourceCodec: Codec<String> = Codec.STRING.optional("container")

private data class SlotRangeSlotSource(val source: String, val slots: List<String>) : SlotSource {
    override fun type() = SlotSourceKeys.SLOT_RANGE
    override fun evaluate(context: DataContext): List<ItemStack> =
        context[SlotSourceInventoryContext].getItems(source, slots)
}

internal data object SlotRangeSlotSourceType : SlotSourceType {
    override fun codec(slotSourceCodec: Codec<SlotSource>): StructCodec<out SlotSource> = StructCodec.struct(
        "source", sourceCodec, { (it as SlotRangeSlotSource).source },
        "slots", slotPatternsCodec, { (it as SlotRangeSlotSource).slots },
        ::SlotRangeSlotSource,
    )
}
