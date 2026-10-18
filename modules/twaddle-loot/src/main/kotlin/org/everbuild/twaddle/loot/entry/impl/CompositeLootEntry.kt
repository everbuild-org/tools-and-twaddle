package org.everbuild.twaddle.loot.entry.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.entry.LootEntryData
import org.everbuild.twaddle.loot.entry.LootEntryKeys
import org.everbuild.twaddle.loot.entry.LootEntryType
import org.everbuild.twaddle.loot.entry.lootEntryCodec
import org.everbuild.twaddle.loot.modifier.LootModifier

data class GroupLootEntryData(val children: List<LootEntry>) : LootEntryData {
    override fun type() = LootEntryKeys.GROUP
}

data class AlternativesLootEntryData(val children: List<LootEntry>) : LootEntryData {
    override fun type() = LootEntryKeys.ALTERNATIVES
}

data class SequenceLootEntryData(val children: List<LootEntry>) : LootEntryData {
    override fun type() = LootEntryKeys.SEQUENCE
}

internal class GroupLootEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ) = lootEntryCodec(
        childrenCodec(entryCodec, GroupLootEntryData::children, ::GroupLootEntryData),
        predicateCodec,
        modifierCodec,
    ) { it.value as GroupLootEntryData }
}

internal class AlternativesLootEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ) = lootEntryCodec(
        childrenCodec(entryCodec, AlternativesLootEntryData::children, ::AlternativesLootEntryData),
        predicateCodec,
        modifierCodec,
    ) { it.value as AlternativesLootEntryData }
}

internal class SequenceLootEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ) = lootEntryCodec(
        childrenCodec(entryCodec, SequenceLootEntryData::children, ::SequenceLootEntryData),
        predicateCodec,
        modifierCodec,
    ) { it.value as SequenceLootEntryData }
}

private fun <T : LootEntryData> childrenCodec(
    entryCodec: Codec<LootEntry>,
    children: (T) -> List<LootEntry>,
    create: (List<LootEntry>) -> T,
): StructCodec<T> = StructCodec.struct(
    "children", entryCodec.list().optional(), { children(it).takeUnless { entries -> entries.isEmpty() } },
    { entries -> create(entries ?: emptyList()) },
)
