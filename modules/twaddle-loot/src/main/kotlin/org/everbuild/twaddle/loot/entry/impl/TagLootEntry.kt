package org.everbuild.twaddle.loot.entry.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.Material
import net.minestom.server.registry.TagKey
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.entry.LootEntryData
import org.everbuild.twaddle.loot.entry.LootEntryKeys
import org.everbuild.twaddle.loot.entry.LootEntryType
import org.everbuild.twaddle.loot.entry.lootEntryCodec
import org.everbuild.twaddle.loot.modifier.LootModifier

/** Entry backed by a tag from Minestom's native material registry. */
data class TagLootEntryData(
    val items: Key,
    val expand: Boolean = false,
) : LootEntryData {
    override fun type() = LootEntryKeys.TAG

    /** Resolves the referenced native material tag, failing if Minestom does not provide it. */
    fun resolveItems(): List<Material> {
        val registry = Material.staticRegistry()
        val tag = registry.getTag(items)
            ?: error("Unknown native material tag #${items.asString()}")
        return tag.map { materialKey ->
            requireNotNull(registry.get(materialKey.key())) {
                "Native material tag #${items.asString()} refers to missing material ${materialKey.key()}"
            }
        }
    }
}

private val materialTagKeyCodec: Codec<Key> = Codec.STRING.transform(
    { TagKey.ofHash<Material>(it).key() },
    { "#${it.asString()}" },
)

internal class TagLootEntryType : LootEntryType {
    override fun codec(
        entryCodec: Codec<LootEntry>,
        predicateCodec: Codec<ContextPredicate>,
        modifierCodec: Codec<LootModifier>,
    ): StructCodec<out LootEntry> {
        val dataCodec = StructCodec.struct(
            "items", materialTagKeyCodec, TagLootEntryData::items,
            "expand", Codec.BOOLEAN.optional(), { it.expand.takeIf { expand -> expand } },
            { items, expand -> TagLootEntryData(items, expand ?: false) },
        )!!
        return lootEntryCodec(dataCodec, predicateCodec, modifierCodec) {
            it.value as TagLootEntryData
        }
    }
}
