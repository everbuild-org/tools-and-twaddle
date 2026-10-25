package org.everbuild.twaddle.loot.entry

import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.loot.LootTable
import org.everbuild.twaddle.loot.modifier.LootModifier
import java.util.Random

/** The result of expanding an entry before a pool selects a weighted candidate. */
data class LootEntryExpansion(
    val succeeded: Boolean,
    val candidates: List<LootEntryCandidate>,
)

/** A weighted entry candidate whose item output is created only when the candidate is selected. */
data class LootEntryCandidate(
    val weight: Int,
    val quality: Int,
    val modifiers: List<LootModifier>,
    val createItems: (DataContext, Random) -> List<ItemStack>,
)

/** Shared runtime services available to registered [LootEntryType] implementations. */
class LootEntryEvaluation internal constructor(
    val context: DataContext,
    val random: Random,
    private val expandEntry: (LootEntry) -> LootEntryExpansion,
    private val generateTable: (Key, LootTable) -> List<ItemStack>,
    private val generateInlineTable: (LootTable) -> List<ItemStack>,
    private val resolveTable: (Key) -> LootTable,
    private val resolveDynamic: (Key) -> List<ItemStack>,
) {
    /** Recursively expands a child entry using the same context and random stream. */
    fun expand(entry: LootEntry): LootEntryExpansion = expandEntry(entry)

    /** Creates the standard single-candidate result for an ordinary leaf entry. */
    fun candidate(
        entry: LootEntry,
        createItems: (DataContext, Random) -> List<ItemStack>,
    ) = LootEntryExpansion(
        succeeded = true,
        candidates = listOf(
            LootEntryCandidate(entry.weight, entry.quality, entry.modifiers, createItems),
        ),
    )

    /** Resolves a table reference now, so missing data is reported even when its candidate loses the roll. */
    fun tableReference(entry: LootEntry, key: Key): LootEntryExpansion {
        val table = resolveTable(key)
        return candidate(entry) { _, _ -> generateTable(key, table) }
    }

    /** Evaluates an inline loot table when its weighted candidate is selected. */
    fun inlineTable(entry: LootEntry, table: LootTable): LootEntryExpansion = candidate(entry) { _, _ ->
        generateInlineTable(table)
    }

    /** Resolves a dynamic drop through the host-supplied runtime hook when the candidate is selected. */
    fun dynamic(entry: LootEntry, key: Key): LootEntryExpansion = candidate(entry) { _, _ ->
        resolveDynamic(key)
    }
}

/** Adds a composite entry's modifiers after the modifiers already attached to its child candidates. */
fun LootEntryExpansion.withParentModifiers(modifiers: List<LootModifier>): LootEntryExpansion = copy(
    candidates = candidates.map { it.copy(modifiers = it.modifiers + modifiers) },
)
