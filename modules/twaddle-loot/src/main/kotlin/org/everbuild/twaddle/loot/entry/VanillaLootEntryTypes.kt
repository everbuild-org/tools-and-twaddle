package org.everbuild.twaddle.loot.entry

import net.kyori.adventure.key.Key
import net.minestom.server.registry.DynamicRegistry
import org.everbuild.twaddle.loot.entry.impl.EmptyLootEntryType
import org.everbuild.twaddle.loot.entry.impl.AlternativesLootEntryType
import org.everbuild.twaddle.loot.entry.impl.DynamicLootEntryType
import org.everbuild.twaddle.loot.entry.impl.GroupLootEntryType
import org.everbuild.twaddle.loot.entry.impl.ItemLootEntryType
import org.everbuild.twaddle.loot.entry.impl.LootTableReferenceEntryType
import org.everbuild.twaddle.loot.entry.impl.SequenceLootEntryType
import org.everbuild.twaddle.loot.entry.impl.TagLootEntryType
import org.everbuild.twaddle.loot.entry.impl.SlotsLootEntryType

/** Registers the simple vanilla loot-entry types implemented by this module. */
fun addAllVanillaLootEntryTypes(registry: DynamicRegistry<LootEntryType>) {
    registry.register(LootEntryKeys.ITEM, ItemLootEntryType())
    registry.register(LootEntryKeys.EMPTY, EmptyLootEntryType())
    registry.register(LootEntryKeys.GROUP, GroupLootEntryType())
    registry.register(LootEntryKeys.ALTERNATIVES, AlternativesLootEntryType())
    registry.register(LootEntryKeys.SEQUENCE, SequenceLootEntryType())
    registry.register(LootEntryKeys.LOOT_TABLE, LootTableReferenceEntryType())
    registry.register(LootEntryKeys.DYNAMIC, DynamicLootEntryType())
    registry.register(LootEntryKeys.TAG, TagLootEntryType())
    registry.register(LootEntryKeys.SLOTS, SlotsLootEntryType())
}

@Suppress("UnstableApiUsage")
fun createVanillaLootEntryTypeRegistry(): DynamicRegistry<LootEntryType> =
    DynamicRegistry.create<LootEntryType>(Key.key("twaddle:vanilla_loot_entry_types")).also(
        ::addAllVanillaLootEntryTypes,
    )
