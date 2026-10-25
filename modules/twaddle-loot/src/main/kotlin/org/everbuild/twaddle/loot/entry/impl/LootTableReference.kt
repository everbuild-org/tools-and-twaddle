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
import org.everbuild.twaddle.loot.entry.LootEntryEvaluation
import org.everbuild.twaddle.loot.entry.LootEntryExpansion
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.LootTable
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource

sealed interface LootTableEntryValue {
    data class Reference(val key: Key) : LootTableEntryValue
    data class Inline(val table: LootTable) : LootTableEntryValue
}

/** Entry that references another loot table by key or embeds a table inline. */
data class LootTableReferenceEntryData(val value: LootTableEntryValue) : LootEntryData {
    constructor(value: Key) : this(LootTableEntryValue.Reference(value))

    override fun type() = LootEntryKeys.LOOT_TABLE
}

internal class LootTableReferenceEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ): StructCodec<out LootEntry> {
        val dataCodec = StructCodec.struct(
            "value", Codec.KEY.transform(
                { LootTableEntryValue.Reference(it) },
                { value ->
                    require(value is LootTableEntryValue.Reference) { "Expected a loot-table key reference, got $value" }
                    value.key
                },
            ), LootTableReferenceEntryData::value,
            ::LootTableReferenceEntryData,
        )!!
        return lootEntryCodec(dataCodec, predicateCodec, modifierCodec) {
            it.value as LootTableReferenceEntryData
        }
    }

    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
        slotSourceCodec: Codec<SlotSource>,
        lootTableCodec: Codec<LootTable>,
    ): StructCodec<out LootEntry> {
        val valueCodec = Codec.Either(Codec.KEY, lootTableCodec).transform(
            { encoded -> encoded.unify({ LootTableEntryValue.Reference(it) }, { LootTableEntryValue.Inline(it) }) },
            { value ->
                when (value) {
                    is LootTableEntryValue.Reference -> net.minestom.server.utils.Either.left(value.key)
                    is LootTableEntryValue.Inline -> net.minestom.server.utils.Either.right(value.table)
                }
            },
        )
        val dataCodec = StructCodec.struct(
            "value", valueCodec, LootTableReferenceEntryData::value,
            ::LootTableReferenceEntryData,
        )!!
        return lootEntryCodec(dataCodec, predicateCodec, modifierCodec) {
            it.value as LootTableReferenceEntryData
        }
    }

    override fun expand(entry: LootEntry, evaluation: LootEntryEvaluation): LootEntryExpansion =
        when (val value = (entry.value as LootTableReferenceEntryData).value) {
            is LootTableEntryValue.Reference -> evaluation.tableReference(entry, value.key)
            is LootTableEntryValue.Inline -> evaluation.inlineTable(entry, value.table)
        }
}
