package org.everbuild.twaddle.loot.entry.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.entry.LootEntryData
import org.everbuild.twaddle.loot.entry.LootEntryKeys
import org.everbuild.twaddle.loot.entry.LootEntryType
import org.everbuild.twaddle.loot.entry.lootEntryCodec
import org.everbuild.twaddle.loot.modifier.LootModifier

/** Entry that produces the item stacks selected by a slot source. */
data class SlotsLootEntryData(val slotSource: SlotSource) : LootEntryData {
    override fun type() = LootEntryKeys.SLOTS
}

internal class SlotsLootEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ): StructCodec<out LootEntry> = codec(
        entryCodec,
        predicateCodec,
        modifierCodec,
        DatapackPrimitives.vanilla().slotSourceCodec,
    )

    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
        slotSourceCodec: Codec<SlotSource>,
    ): StructCodec<out LootEntry> {
        val dataCodec = StructCodec.struct(
            "slot_source", slotSourceCodec, SlotsLootEntryData::slotSource,
            ::SlotsLootEntryData,
        )!!
        return lootEntryCodec(dataCodec, predicateCodec, modifierCodec) {
            it.value as SlotsLootEntryData
        }
    }
}
