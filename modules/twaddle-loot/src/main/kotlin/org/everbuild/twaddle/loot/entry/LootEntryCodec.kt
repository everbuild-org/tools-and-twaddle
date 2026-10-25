package org.everbuild.twaddle.loot.entry

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.loot.LootTable

/** Builds a recursive codec for registered entry types with vanilla's shared entry fields. */
fun createLootEntryCodec(
    registry: Registry<LootEntryType>,
    predicateCodec: Codec<ContextPredicate>,
    modifierCodec: Codec<LootModifier>,
): Codec<LootEntry> = createLootEntryCodec(
    registry,
    predicateCodec,
    modifierCodec,
    DatapackPrimitives.vanilla().slotSourceCodec,
)

/** Builds a recursive entry codec using the supplied slot-source codec and registered entry types. */
fun createLootEntryCodec(
    registry: Registry<LootEntryType>,
    predicateCodec: Codec<ContextPredicate>,
    modifierCodec: Codec<LootModifier>,
    slotSourceCodec: Codec<SlotSource>,
): Codec<LootEntry> = createLootEntryCodecInternal(registry, predicateCodec, modifierCodec, slotSourceCodec, null)

/** Builds a recursive entry codec that can decode inline loot-table values as well. */
fun createLootEntryCodec(
    registry: Registry<LootEntryType>,
    predicateCodec: Codec<ContextPredicate>,
    modifierCodec: Codec<LootModifier>,
    slotSourceCodec: Codec<SlotSource>,
    lootTableCodec: Codec<LootTable>,
): Codec<LootEntry> = createLootEntryCodecInternal(registry, predicateCodec, modifierCodec, slotSourceCodec, lootTableCodec)

private fun createLootEntryCodecInternal(
    registry: Registry<LootEntryType>,
    predicateCodec: Codec<ContextPredicate>,
    modifierCodec: Codec<LootModifier>,
    slotSourceCodec: Codec<SlotSource>,
    lootTableCodec: Codec<LootTable>?,
): Codec<LootEntry> = Codec.Recursive { entryCodec ->
    Codec.STRING.unionType(
        "type",
        { id ->
            val type = registry.get(Key.key(id)) ?: return@unionType null
            if (lootTableCodec == null) type.codec(entryCodec, predicateCodec, modifierCodec, slotSourceCodec)
            else type.codec(entryCodec, predicateCodec, modifierCodec, slotSourceCodec, lootTableCodec)
        },
        { entry -> registry.getKey(entry.value.type())?.key()?.asString() },
    )
}

/** Adds entry fields shared by vanilla entry variants to a variant-specific struct codec. */
internal fun <T : LootEntryData> lootEntryCodec(
    dataCodec: StructCodec<T>,
    predicateCodec: Codec<ContextPredicate>,
    modifierCodec: Codec<LootModifier>,
    data: (LootEntry) -> T,
): StructCodec<LootEntry> = StructCodec.struct(
    StructCodec.INLINE, dataCodec, data,
    "weight", Codec.INT.optional(), { it.weight.takeUnless { weight -> weight == 1 } },
    "quality", Codec.INT.optional(), { it.quality.takeUnless { quality -> quality == 0 } },
    "condition", predicateCodec.listOrSingle().optional(emptyList()), { it.predicates },
    "modifier", modifierCodec.listOrSingle().optional(emptyList()), { it.modifiers },
) { value, weight, quality, conditions, functions ->
    LootEntry(value, weight ?: 1, quality ?: 0, conditions, functions)
}
