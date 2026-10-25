package org.everbuild.twaddle.loot

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.metadata.other.FishingHookMeta
import net.minestom.server.event.entity.EntityDeathEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.inventory.EquipmentHandler
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.BlockPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DefaultRandomSequenceController
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.ItemPredicateMatcherContext
import org.everbuild.twaddle.datapack_primitives.context.LocationContextKey
import org.everbuild.twaddle.datapack_primitives.context.RandomSequenceController
import org.everbuild.twaddle.datapack_primitives.context.RegistryEntryReference
import org.everbuild.twaddle.datapack_primitives.context.ToolPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.withEnchantment
import org.everbuild.twaddle.datapack_primitives.context.withEnvironment
import org.everbuild.twaddle.datapack_primitives.context.withExplosionRadius
import org.everbuild.twaddle.datapack_primitives.context.withKilledByPlayer
import org.everbuild.twaddle.datapack_primitives.context.withRandomSequenceController
import org.everbuild.twaddle.loot.modifier.withLootBlockStateAccess
import org.everbuild.twaddle.loot.modifier.withLootModifierRegistry
import org.everbuild.twaddle.loot.modifier.context.LootComponentSourceAccessContext
import org.everbuild.twaddle.loot.modifier.context.withLootModifierRegistries

/** Resolves a structure id or tag at a position when the host provides structure data. */
fun interface StructurePredicateMatcher {
    fun matches(position: ContextLocation, structure: RegistryEntryReference): Boolean
}

/**
 * Creates a context for breaking [block] at [blockPosition]. Positions are converted to block
 * centers, matching vanilla's block-loot origin.
 */
@JvmOverloads
fun DataContext.Companion.blockBreak(
    instance: Instance,
    block: Block,
    blockPosition: Point,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    tool: ItemStack = ItemStack.AIR,
    entity: Entity? = null,
    entityTargets: Map<String, Entity> = entity?.let(::blockBreakEntityTargets).orEmpty(),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    explosionRadius: Float? = null,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext = createVanillaContext(
    instance = instance,
    block = block,
    origin = blockCenter(blockPosition),
    support = support,
    tool = tool,
    entityTargets = entityTargets,
    randomSequences = randomSequences,
    lootLuck = lootLuck,
    explosionRadius = explosionRadius,
    killedByPlayer = entity is Player,
    structureMatcher = structureMatcher,
)

/** Creates a block-break context using data available from [event]. */
@JvmOverloads
fun DataContext.Companion.blockBreak(
    event: PlayerBlockBreakEvent,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    explosionRadius: Float? = null,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext {
    val player = event.player
    return blockBreak(
        instance = event.instance,
        block = event.block,
        blockPosition = event.blockPosition,
        support = support,
        tool = player.itemInMainHand,
        entity = player,
        entityTargets = blockBreakEntityTargets(player),
        randomSequences = randomSequences,
        lootLuck = lootLuck,
        explosionRadius = explosionRadius,
        structureMatcher = structureMatcher,
    )
}

/**
 * Creates the vanilla `entity` loot context for [entity]'s death. Supply [damage] when available
 * so attacker roles and the looting tool are populated; Minestom's [EntityDeathEvent] does not
 * include that information.
 */
@JvmOverloads
fun DataContext.Companion.entityDeath(
    entity: Entity,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    damage: Damage? = null,
    attackingPlayer: Player? = damage?.attacker as? Player,
    tool: ItemStack = deathLootTool(damage),
    origin: Point = entity.position,
    entityTargets: Map<String, Entity> = entityDeathEntityTargets(entity, damage, attackingPlayer),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext {
    val instance = requireNotNull(entity.instance) { "Entity loot needs an entity attached to an instance" }
    return createVanillaContext(
        instance = instance,
        origin = location(origin),
        support = support,
        tool = tool,
        entityTargets = entityTargets,
        randomSequences = randomSequences,
        lootLuck = lootLuck,
        killedByPlayer = attackingPlayer != null,
        structureMatcher = structureMatcher,
    )
}

/** Creates an entity-death context from [event], with optional damage data captured by the host. */
@JvmOverloads
fun DataContext.Companion.entityDeath(
    event: EntityDeathEvent,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    damage: Damage? = null,
    attackingPlayer: Player? = damage?.attacker as? Player,
    tool: ItemStack = deathLootTool(damage),
    origin: Point = event.entity.position,
    entityTargets: Map<String, Entity> = entityDeathEntityTargets(event.entity, damage, attackingPlayer),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext = entityDeath(
    entity = event.entity,
    support = support,
    damage = damage,
    attackingPlayer = attackingPlayer,
    tool = tool,
    origin = origin,
    entityTargets = entityTargets,
    randomSequences = randomSequences,
    lootLuck = lootLuck,
    structureMatcher = structureMatcher,
)

/**
 * Creates the vanilla `chest` loot context for a loot container at [containerBlockPosition].
 * The position is converted to the container's block center.
 */
@JvmOverloads
fun DataContext.Companion.containerLoot(
    instance: Instance,
    containerBlockPosition: Point,
    opener: Entity? = null,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    entityTargets: Map<String, Entity> = opener?.let(::containerOpenerEntityTargets).orEmpty(),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext = createVanillaContext(
    instance = instance,
    origin = blockCenter(containerBlockPosition),
    support = support,
    entityTargets = entityTargets,
    randomSequences = randomSequences,
    lootLuck = lootLuck,
    structureMatcher = structureMatcher,
)

/** Creates a chest-loot context from [event], using the supplied container block position. */
@JvmOverloads
fun DataContext.Companion.containerLoot(
    event: InventoryOpenEvent,
    containerBlockPosition: Point,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    entityTargets: Map<String, Entity> = containerOpenerEntityTargets(event.player),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext {
    val instance = requireNotNull(event.player.instance) { "Container loot needs the opener attached to an instance" }
    return containerLoot(
        instance = instance,
        containerBlockPosition = containerBlockPosition,
        opener = event.player,
        support = support,
        entityTargets = entityTargets,
        randomSequences = randomSequences,
        lootLuck = lootLuck,
        structureMatcher = structureMatcher,
    )
}

/**
 * Creates the vanilla `fishing` loot context from a fishing bobber. The bobber's position and
 * instance provide the origin and world; pass [tool] if the owner no longer holds the cast rod.
 */
@JvmOverloads
fun DataContext.Companion.fishing(
    fishingHook: Entity,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    tool: ItemStack = fishingRod(fishingHook),
    origin: Point = fishingHook.position,
    entityTargets: Map<String, Entity> = fishingEntityTargets(fishingHook),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext {
    val instance = requireNotNull(fishingHook.instance) { "Fishing loot needs a fishing hook attached to an instance" }
    return createVanillaContext(
        instance = instance,
        origin = location(origin),
        support = support,
        tool = tool,
        entityTargets = entityTargets,
        randomSequences = randomSequences,
        lootLuck = lootLuck,
        structureMatcher = structureMatcher,
    )
}

/**
 * Creates the vanilla `block_interact` context for interacting with [block] at [blockPosition].
 * Positions are converted to block centers.
 */
@JvmOverloads
fun DataContext.Companion.blockInteract(
    instance: Instance,
    block: Block,
    blockPosition: Point,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    interactingEntity: Entity? = null,
    tool: ItemStack = interactingEntity?.mainHandItem() ?: ItemStack.AIR,
    entityTargets: Map<String, Entity> = interactingEntity?.let(::interactingEntityTargets).orEmpty(),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext = createVanillaContext(
    instance = instance,
    block = block,
    origin = blockCenter(blockPosition),
    support = support,
    tool = tool,
    entityTargets = entityTargets,
    randomSequences = randomSequences,
    lootLuck = lootLuck,
    structureMatcher = structureMatcher,
)

/** Creates a block-interaction context using data available from [event]. */
@JvmOverloads
fun DataContext.Companion.blockInteract(
    event: PlayerBlockInteractEvent,
    support: VanillaLootSupport = VanillaLootSupport.vanilla(),
    randomSequences: RandomSequenceController = DefaultRandomSequenceController(),
    lootLuck: Float = 0f,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext = blockInteract(
    instance = event.instance,
    block = event.block,
    blockPosition = event.blockPosition,
    support = support,
    interactingEntity = event.player,
    tool = event.player.getItemInHand(event.hand),
    entityTargets = interactingEntityTargets(event.player),
    randomSequences = randomSequences,
    lootLuck = lootLuck,
    structureMatcher = structureMatcher,
)

private fun createVanillaContext(
    instance: Instance,
    origin: ContextLocation,
    support: VanillaLootSupport,
    tool: ItemStack? = null,
    entityTargets: Map<String, Entity> = emptyMap(),
    randomSequences: RandomSequenceController,
    lootLuck: Float,
    block: Block? = null,
    explosionRadius: Float? = null,
    killedByPlayer: Boolean? = null,
    structureMatcher: StructurePredicateMatcher? = null,
): DataContext {
    val registries = instance.registries()
    val access = MinestomLootContextAccess(
        instance = instance,
        registries = registries,
        block = block,
        tool = tool,
        origin = origin,
        entityTargets = entityTargets,
        structureMatcher = structureMatcher,
    )
    val context = DataContext()
        .withEnvironment(instance)
        .withLootModifierRegistries(registries)
        .withLootModifierRegistry(support.modifierTypes)
        .withRandomSequenceController(randomSequences)
        .withLootLuck(lootLuck)
        .withDefaultBehaviour(ItemPredicateMatcherContext) { access.itemPredicateMatcher }
        .withDefaultBehaviour(EntityPredicateContext) { access.entityPredicateMatcher }
        .withDefaultBehaviour(LocationContextKey) { access.locationContext }

    if (tool != null) {
        context.withEnchantment(tool)
        access.toolPredicateMatcher?.let { matcher ->
            context.withDefaultBehaviour(ToolPredicateContext) { matcher }
        }
    }
    if (killedByPlayer != null) context.withKilledByPlayer(killedByPlayer)
    if (block != null) {
        context.withLootBlockStateAccess { block }
        context.withExplosionRadius(explosionRadius)
        context.withDefaultBehaviour(LootComponentSourceAccessContext) { access.componentSourceAccess }
        context.withDefaultBehaviour(DynamicLootResolverContext) { access.dynamicLootResolver }
        access.blockPredicateMatcher?.let { matcher ->
            context.withDefaultBehaviour(BlockPredicateContext) { matcher }
        }
    }
    return context
}

private fun blockBreakEntityTargets(entity: Entity): Map<String, Entity> = buildMap {
    put("this", entity)
    put("killer", entity)
    put("direct_killer", entity)
    put("attacker", entity)
    put("direct_attacker", entity)
    if (entity is Player) {
        put("killer_player", entity)
        put("attacking_player", entity)
    }
}

private fun entityDeathEntityTargets(
    entity: Entity,
    damage: Damage?,
    attackingPlayer: Player?,
): Map<String, Entity> = buildMap {
    put("this", entity)
    damage?.attacker?.let {
        put("attacker", it)
        put("killer", it)
    }
    damage?.source?.let {
        put("direct_attacker", it)
        put("direct_killer", it)
    }
    attackingPlayer?.let {
        put("attacking_player", it)
        put("killer_player", it)
    }
}

private fun containerOpenerEntityTargets(entity: Entity): Map<String, Entity> = mapOf("this" to entity)

private fun interactingEntityTargets(entity: Entity): Map<String, Entity> = mapOf("interacting_entity" to entity)

private fun fishingEntityTargets(fishingHook: Entity): Map<String, Entity> = mapOf("this" to fishingHook)

private fun deathLootTool(damage: Damage?): ItemStack =
    damage?.attacker?.mainHandItem()
        ?: damage?.source?.mainHandItem()
        ?: ItemStack.AIR

private fun fishingRod(fishingHook: Entity): ItemStack {
    val owner = (fishingHook.entityMeta as? FishingHookMeta)?.ownerEntity
    return owner?.mainHandItem() ?: ItemStack.AIR
}

private fun Entity.mainHandItem(): ItemStack? =
    (this as? EquipmentHandler)?.getEquipment(EquipmentSlot.MAIN_HAND)

private fun blockCenter(position: Point) = ContextLocation(
    position.x() + 0.5,
    position.y() + 0.5,
    position.z() + 0.5,
)

private fun location(position: Point) = ContextLocation(position.x(), position.y(), position.z())
