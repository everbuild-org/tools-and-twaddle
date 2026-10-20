package org.everbuild.twaddle.loot.entry.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.entry.*
import org.everbuild.twaddle.loot.modifier.LootModifier

/** Entry that delegates item production to a dynamic drop source. */
data class DynamicLootEntryData(val name: Key) : LootEntryData {
    override fun type() = LootEntryKeys.DYNAMIC
}

internal class DynamicLootEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ): StructCodec<out LootEntry> {
        val dataCodec = StructCodec.struct(
            "name", Codec.KEY, DynamicLootEntryData::name,
            ::DynamicLootEntryData,
        )!!
        return lootEntryCodec(dataCodec, predicateCodec, modifierCodec) {
            it.value as DynamicLootEntryData
        }
    }
}
