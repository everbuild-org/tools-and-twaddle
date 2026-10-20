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

data class ItemLootEntryData(val name: Key) : LootEntryData {
    override fun type() = LootEntryKeys.ITEM
}

internal class ItemLootEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ): StructCodec<out LootEntry> {
        val dataCodec = StructCodec.struct(
            "name", Codec.KEY, ItemLootEntryData::name,
            ::ItemLootEntryData,
        )!!
        return lootEntryCodec(dataCodec, predicateCodec, modifierCodec) { it.value as ItemLootEntryData }
    }
}
