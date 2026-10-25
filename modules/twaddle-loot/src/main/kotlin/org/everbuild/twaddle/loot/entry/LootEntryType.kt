package org.everbuild.twaddle.loot.entry

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.loot.LootTable

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

    /** Builds this entry codec with support for entries that embed another loot table. */
    fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
        slotSourceCodec: Codec<SlotSource>,
        lootTableCodec: Codec<LootTable>,
    ): StructCodec<out LootEntry> = codec(entryCodec, predicateCodec, modifierCodec, slotSourceCodec)

    /** Expands an entry into weighted candidates before the pool performs its roll selection. */
    fun expand(entry: LootEntry, evaluation: LootEntryEvaluation): LootEntryExpansion
}
