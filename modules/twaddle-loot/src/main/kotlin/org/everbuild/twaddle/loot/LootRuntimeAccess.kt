package org.everbuild.twaddle.loot

import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DataContextKey
import java.util.Random

/** Resolves a loot-table key used by a `minecraft:loot_table` entry. */
fun interface LootTableResolver {
    fun resolve(key: Key): LootTable?
}

val LootTableResolverContext = DataContextKey<LootTableResolver>("twaddle:loot_table_resolver")

@JvmSynthetic
fun DataContext.withLootTableResolver(resolver: LootTableResolver) =
    withBehaviour(LootTableResolverContext, resolver)

/** Produces a dynamic loot drop such as `minecraft:contents` for the active loot context. */
fun interface DynamicLootResolver {
    fun resolve(name: Key, context: DataContext, random: Random): List<ItemStack>
}

val DynamicLootResolverContext = DataContextKey<DynamicLootResolver>("twaddle:dynamic_loot_resolver")

@JvmSynthetic
fun DataContext.withDynamicLootResolver(resolver: DynamicLootResolver) =
    withBehaviour(DynamicLootResolverContext, resolver)

/** Luck used when calculating a pool's bonus rolls and quality-adjusted entry weights. */
val LootLuckContext = DataContextKey<Float>("twaddle:loot_luck")

@JvmSynthetic
fun DataContext.withLootLuck(luck: Float) = withBehaviour(LootLuckContext, luck)
