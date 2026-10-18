package org.everbuild.twaddle.datapack_primitives.slot_source

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.slot_source.impl.GroupSlotSource

/** Builds a recursive codec for registered slot sources. A list is accepted as a group shorthand. */
fun createSlotSourceCodec(registry: Registry<SlotSourceType>): Codec<SlotSource> = Codec.Recursive { slotSourceCodec ->
    val typedCodec = Codec.STRING.unionType(
        "type",
        { id -> registry.get(Key.key(id))?.codec(slotSourceCodec) },
        { source -> registry.getKey(source.type())?.key()?.asString() },
    )
    Codec.Either<List<SlotSource>, SlotSource>(slotSourceCodec.list(), typedCodec).transform(
        { encoded ->
            encoded.unify<SlotSource>(
                { sources -> GroupSlotSource(sources) },
                { source -> source },
            )
        },
        { source ->
            if (source is GroupSlotSource) {
                net.minestom.server.utils.Either.left<List<SlotSource>, SlotSource>(source.sources)
            } else {
                net.minestom.server.utils.Either.right<List<SlotSource>, SlotSource>(source)
            }
        },
    )
}
