package org.everbuild.twaddle.loot

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.entry.LootEntryType
import org.everbuild.twaddle.loot.entry.createLootEntryCodec
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.loot.modifier.createLootModifierCodec
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource

/** Builds a loot-table codec using the caller's registered entry and modifier types. */
fun createLootTableCodec(
    predicateCodec: Codec<ContextPredicate>,
    entryTypes: Registry<LootEntryType>,
    modifierTypes: Registry<LootModifierType>,
    intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
    floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
): Codec<LootTable> = createLootTableCodec(
    predicateCodec,
    entryTypes,
    modifierTypes,
    intNumberProviderCodec,
    floatNumberProviderCodec,
    DatapackPrimitives.vanilla().slotSourceCodec,
)

/** Builds a loot-table codec using the caller's registered slot-source types as well. */
fun createLootTableCodec(
    predicateCodec: Codec<ContextPredicate>,
    entryTypes: Registry<LootEntryType>,
    modifierTypes: Registry<LootModifierType>,
    intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
    floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    slotSourceCodec: Codec<SlotSource>,
): Codec<LootTable> {
    return Codec.Recursive { lootTableCodec ->
        val modifierCodec = createLootModifierCodec(
            modifierTypes,
            predicateCodec,
            intNumberProviderCodec,
            floatNumberProviderCodec,
        )
        val entryCodec = createLootEntryCodec(entryTypes, predicateCodec, modifierCodec, slotSourceCodec, lootTableCodec)
        val poolCodec = createLootPoolCodec(
            entryCodec,
            predicateCodec,
            modifierCodec,
            intNumberProviderCodec,
            floatNumberProviderCodec,
        )

        StructCodec.struct(
            "type", Codec.STRING.optional("generic"), LootTable::type,
            "pools", poolCodec.list().optional(emptyList()), LootTable::pools,
            "random_sequence", Codec.STRING.optional(), LootTable::randomSequence,
            "modifier", modifierCodec.listOrSingle().optional(emptyList()), LootTable::modifiers,
        ) { type, pools, randomSequence, modifiers ->
            LootTable(type, pools, randomSequence, modifiers)
        }
    }
}

/** Builds the pool codec with shared predicates, modifiers, and number-provider codecs. */
fun createLootPoolCodec(
    entryCodec: Codec<LootEntry>,
    predicateCodec: Codec<ContextPredicate>,
    modifierCodec: Codec<LootModifier>,
    intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
    floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
): StructCodec<LootPool> = StructCodec.struct(
    "rolls", intNumberProviderCodec, LootPool::rolls,
    "entries", entryCodec.list(), LootPool::entries,
    "bonus_rolls", floatNumberProviderCodec.optional(), LootPool::bonusRolls,
    "condition", predicateCodec.listOrSingle().optional(emptyList()), { it.predicates },
    "modifier", modifierCodec.listOrSingle().optional(emptyList()), { it.modifiers },
    ::LootPool,
)
