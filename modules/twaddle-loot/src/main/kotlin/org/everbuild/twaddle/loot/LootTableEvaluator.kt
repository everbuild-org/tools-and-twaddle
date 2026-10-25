package org.everbuild.twaddle.loot

import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.RandomSequenceControllerContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.entry.LootEntryCandidate
import org.everbuild.twaddle.loot.entry.LootEntryEvaluation
import org.everbuild.twaddle.loot.entry.LootEntryExpansion
import org.everbuild.twaddle.loot.entry.LootEntryType
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.loot.modifier.applyLootModifier
import java.util.IdentityHashMap
import java.util.Random

/** Evaluates loot tables using the caller's entry and modifier registries. */
class LootTableEvaluator(
    private val entryTypes: Registry<LootEntryType>,
    private val modifierTypes: Registry<LootModifierType>,
) {
    /** Generates loot using [random] unless a table declares a named random sequence. */
    @JvmOverloads
    fun generate(table: LootTable, context: DataContext, random: Random = Random()): List<ItemStack> {
        return Evaluation(context).generate(table, random, null)
    }

    private inner class Evaluation(private val context: DataContext) {
        private val activeTables = IdentityHashMap<LootTable, Boolean>()
        private val activeKeys = LinkedHashSet<Key>()

        fun generate(table: LootTable, inheritedRandom: Random, key: Key?): List<ItemStack> {
            if (activeTables.put(table, true) != null) {
                error("Recursive loot table reference detected${key?.let { " at $it" } ?: ""}")
            }
            if (key != null && !activeKeys.add(key)) {
                activeTables.remove(table)
                error("Recursive loot table reference detected for $key")
            }

            try {
                val random = randomFor(table, inheritedRandom)
                val items = mutableListOf<ItemStack>()
                for (pool in table.pools) {
                    if (!testAll(pool.predicates, random)) continue

                    val entryEvaluation = LootEntryEvaluation(
                        context = context,
                        random = random,
                        expandEntry = { entry -> expand(entry, random) },
                        generateTable = { referenced, referencedTable ->
                            generate(referencedTable, random, referenced)
                        },
                        generateInlineTable = { inlineTable -> generate(inlineTable, random, null) },
                        resolveTable = ::resolveTable,
                        resolveDynamic = { name -> resolveDynamic(name, random) },
                    )
                    val candidates = pool.entries.flatMap { entryEvaluation.expand(it).candidates }
                    val luck = context.getBehaviourOrNull(LootLuckContext) ?: 0f
                    val rolls = pool.rollCount(context, random, luck)

                    for (roll in 0 until rolls) {
                        val candidate = choose(candidates, luck, random) ?: break
                        for (item in candidate.createItems(context, random)) {
                            applyModifiers(item, candidate.modifiers + pool.modifiers, random)?.let(items::add)
                        }
                    }
                }
                return applyModifiers(items, table.modifiers, random)
            } finally {
                activeTables.remove(table)
                if (key != null) activeKeys.remove(key)
            }
        }

        private fun randomFor(table: LootTable, inheritedRandom: Random): Random {
            val sequence = table.randomSequence ?: return inheritedRandom
            val key = try {
                Key.key(sequence)
            } catch (failure: IllegalArgumentException) {
                throw IllegalArgumentException("Invalid loot table random_sequence '$sequence'", failure)
            }
            val controller = context.getBehaviourOrNull(RandomSequenceControllerContext)
                ?: error("Loot table declares random_sequence '$sequence' but no RandomSequenceController is in DataContext")
            return controller.getRandom(key)
        }

        private fun expand(entry: LootEntry, random: Random): LootEntryExpansion {
            if (!testAll(entry.predicates, random)) return LootEntryExpansion(false, emptyList())
            val type = entryTypes.get(entry.value.type())
                ?: error("No loot entry type registered for ${entry.value.type()}")
            return type.expand(entry, LootEntryEvaluation(
                context = context,
                random = random,
                expandEntry = { child -> expand(child, random) },
                generateTable = { key, referencedTable -> generate(referencedTable, random, key) },
                generateInlineTable = { inlineTable -> generate(inlineTable, random, null) },
                resolveTable = ::resolveTable,
                resolveDynamic = { name -> resolveDynamic(name, random) },
            ))
        }

        private fun testAll(predicates: List<ContextPredicate>, random: Random): Boolean =
            predicates.all { it.test(context, random) }

        private fun resolveTable(key: Key): LootTable {
            val resolver = context.getBehaviourOrNull(LootTableResolverContext)
                ?: error("Loot table entry references $key but no LootTableResolver is in DataContext")
            return resolver.resolve(key) ?: error("No loot table found for reference $key")
        }

        private fun resolveDynamic(key: Key, random: Random): List<ItemStack> {
            val resolver = context.getBehaviourOrNull(DynamicLootResolverContext)
                ?: error("Dynamic loot entry references $key but no DynamicLootResolver is in DataContext")
            return resolver.resolve(key, context, random)
        }

        private fun applyModifiers(
            item: ItemStack,
            modifiers: List<LootModifier>,
            random: Random,
        ): ItemStack? {
            var current: ItemStack? = item
            for (modifier in modifiers) {
                val before = current ?: return null
                if (before.isAir) return null
                current = applyLootModifier(modifier, before, modifierTypes, context, random)
            }
            return current?.takeUnless(ItemStack::isAir)
        }

        private fun applyModifiers(
            items: List<ItemStack>,
            modifiers: List<LootModifier>,
            random: Random,
        ): List<ItemStack> = items.mapNotNull { applyModifiers(it, modifiers, random) }
    }

    private fun choose(candidates: List<LootEntryCandidate>, luck: Float, random: Random): LootEntryCandidate? {
        if (candidates.isEmpty()) return null
        val weights = candidates.map { candidate ->
            floorWeight(candidate.weight.toDouble() + candidate.quality.toDouble() * luck)
        }
        val total = weights.fold(0L, Math::addExact)
        if (total <= 0L) return null

        var remaining = random.nextLong(total)
        for ((index, weight) in weights.withIndex()) {
            if (remaining < weight) return candidates[index]
            remaining -= weight
        }
        error("Weighted loot selection exceeded total weight")
    }

    private fun floorWeight(value: Double): Long {
        if (value.isNaN() || value <= 0.0) return 0L
        if (value >= Long.MAX_VALUE.toDouble()) return Long.MAX_VALUE
        return kotlin.math.floor(value).toLong()
    }

    private fun LootPool.rollCount(context: DataContext, random: Random, luck: Float): Int {
        val base = rolls.get(context, random).toLong()
        val bonus = bonusRolls?.get(context, random)?.toDouble()?.times(luck.toDouble()) ?: 0.0
        val bonusCount = if (bonus.isNaN()) 0L else kotlin.math.floor(bonus).toLong()
        return (base + bonusCount).coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
    }
}
