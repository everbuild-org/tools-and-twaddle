package org.everbuild.twaddle.loot.modifier

import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DataContextKey

/** Provides the block state that is the source of a block loot operation. */
fun interface LootBlockStateAccess {
    fun blockState(): Block?
}

val LootBlockStateContext = DataContextKey<LootBlockStateAccess>("twaddle:loot_block_state")

@JvmSynthetic
fun DataContext.withLootBlockStateAccess(access: LootBlockStateAccess) =
    withBehaviour(LootBlockStateContext, access)

/** Resolves the recipe output for a furnace-smeltable item, or null when no recipe matches. */
fun interface FurnaceSmeltAccess {
    fun result(input: ItemStack): ItemStack?
}

val FurnaceSmeltContext = DataContextKey<FurnaceSmeltAccess>("twaddle:furnace_smelt")

@JvmSynthetic
fun DataContext.withFurnaceSmeltAccess(access: FurnaceSmeltAccess) =
    withBehaviour(FurnaceSmeltContext, access)

/** Request passed to a host that can locate structures and manage map data. */
data class ExplorationMapRequest(
    val destination: String,
    val decoration: String,
    val zoom: Int,
    val searchRadius: Int,
    val skipExistingChunks: Boolean,
)

/** Implements structure search and map-instance creation, which Minestom does not provide. */
fun interface ExplorationMapAccess {
    /** Returns the item with its new map instance, or null if no destination was found. */
    fun createMap(item: ItemStack, request: ExplorationMapRequest, context: DataContext): ItemStack?
}

val ExplorationMapContext = DataContextKey<ExplorationMapAccess>("twaddle:exploration_map")

@JvmSynthetic
fun DataContext.withExplorationMapAccess(access: ExplorationMapAccess) =
    withBehaviour(ExplorationMapContext, access)

val LootModifierRegistryContext = DataContextKey<Registry<LootModifierType>>("twaddle:loot_modifier_registry")

@JvmSynthetic
fun DataContext.withLootModifierRegistry(registry: Registry<LootModifierType>) =
    withBehaviour(LootModifierRegistryContext, registry)
