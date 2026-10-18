package org.everbuild.twaddle.datapack_primitives.slot_source

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec

/** Supplies the codec for one registered slot-source type. */
interface SlotSourceType {
    fun codec(slotSourceCodec: Codec<SlotSource>): StructCodec<out SlotSource>
}
