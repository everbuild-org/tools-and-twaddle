package org.everbuild.twaddle.loot.entry

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource

/** Supplies a codec for one registered loot-entry type. */
interface LootEntryType {
    fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ): StructCodec<out LootEntry>

    /** Builds this entry codec with the configured slot-source codec, when the entry needs it. */
    fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
        slotSourceCodec: Codec<SlotSource>,
    ): StructCodec<out LootEntry> = codec(entryCodec, predicateCodec, modifierCodec)
}
