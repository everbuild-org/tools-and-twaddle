package org.everbuild.twaddle.loot.entry.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.entry.LootEntryData
import org.everbuild.twaddle.loot.entry.LootEntryKeys
import org.everbuild.twaddle.loot.entry.LootEntryType
import org.everbuild.twaddle.loot.entry.lootEntryCodec
import org.everbuild.twaddle.loot.modifier.LootModifier

/** Entry that references another loot table by resource key. */
data class LootTableReferenceEntryData(val value: Key) : LootEntryData {
    override fun type() = LootEntryKeys.LOOT_TABLE
}

internal class LootTableReferenceEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ): StructCodec<out LootEntry> {
        val dataCodec = StructCodec.struct(
            "value", Codec.KEY, LootTableReferenceEntryData::value,
            ::LootTableReferenceEntryData,
        )!!
        return lootEntryCodec(dataCodec, predicateCodec, modifierCodec) {
            it.value as LootTableReferenceEntryData
        }
    }
}
