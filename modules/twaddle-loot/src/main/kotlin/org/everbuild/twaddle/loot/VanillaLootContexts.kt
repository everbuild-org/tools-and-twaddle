package org.everbuild.twaddle.loot

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.entity.Player
import net.minestom.server.entity.damage.Damage
import net.minestom.server.event.entity.EntityDeathEvent
import net.minestom.server.event.inventory.InventoryOpenEvent
import net.minestom.server.event.player.PlayerBlockBreakEvent
import net.minestom.server.event.player.PlayerBlockInteractEvent
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.RandomSequenceController

/** Java-friendly static entry points for the vanilla [DataContext] factories. */
object VanillaLootContexts {
    @JvmStatic
    fun blockBreak(instance: Instance, block: Block, blockPosition: Point): DataContext =
        DataContext.blockBreak(instance, block, blockPosition)

    @JvmStatic
    fun blockBreak(
        instance: Instance,
        block: Block,
        blockPosition: Point,
        support: VanillaLootSupport,
        tool: ItemStack,
        entity: Entity?,
        entityTargets: Map<String, Entity>,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        explosionRadius: Float?,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.blockBreak(
        instance,
        block,
        blockPosition,
        support,
        tool,
        entity,
        entityTargets,
        randomSequences,
        lootLuck,
        explosionRadius,
        structureMatcher,
    )

    @JvmStatic
    fun blockBreak(event: PlayerBlockBreakEvent): DataContext = DataContext.blockBreak(event)

    @JvmStatic
    fun blockBreak(
        event: PlayerBlockBreakEvent,
        support: VanillaLootSupport,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        explosionRadius: Float?,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.blockBreak(
        event,
        support,
        randomSequences,
        lootLuck,
        explosionRadius,
        structureMatcher,
    )

    @JvmStatic
    fun entityDeath(entity: Entity): DataContext = DataContext.entityDeath(entity)

    @JvmStatic
    fun entityDeath(
        entity: Entity,
        support: VanillaLootSupport,
        damage: Damage?,
        attackingPlayer: Player?,
        tool: ItemStack,
        origin: Point,
        entityTargets: Map<String, Entity>,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.entityDeath(
        entity,
        support,
        damage,
        attackingPlayer,
        tool,
        origin,
        entityTargets,
        randomSequences,
        lootLuck,
        structureMatcher,
    )

    @JvmStatic
    fun entityDeath(event: EntityDeathEvent): DataContext = DataContext.entityDeath(event)

    @JvmStatic
    fun entityDeath(
        event: EntityDeathEvent,
        support: VanillaLootSupport,
        damage: Damage?,
        attackingPlayer: Player?,
        tool: ItemStack,
        origin: Point,
        entityTargets: Map<String, Entity>,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.entityDeath(
        event,
        support,
        damage,
        attackingPlayer,
        tool,
        origin,
        entityTargets,
        randomSequences,
        lootLuck,
        structureMatcher,
    )

    @JvmStatic
    fun containerLoot(instance: Instance, containerBlockPosition: Point): DataContext =
        DataContext.containerLoot(instance, containerBlockPosition)

    @JvmStatic
    fun containerLoot(
        instance: Instance,
        containerBlockPosition: Point,
        opener: Entity?,
        support: VanillaLootSupport,
        entityTargets: Map<String, Entity>,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.containerLoot(
        instance,
        containerBlockPosition,
        opener,
        support,
        entityTargets,
        randomSequences,
        lootLuck,
        structureMatcher,
    )

    @JvmStatic
    fun containerLoot(event: InventoryOpenEvent, containerBlockPosition: Point): DataContext =
        DataContext.containerLoot(event, containerBlockPosition)

    @JvmStatic
    fun containerLoot(
        event: InventoryOpenEvent,
        containerBlockPosition: Point,
        support: VanillaLootSupport,
        entityTargets: Map<String, Entity>,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.containerLoot(
        event,
        containerBlockPosition,
        support,
        entityTargets,
        randomSequences,
        lootLuck,
        structureMatcher,
    )

    @JvmStatic
    fun fishing(fishingHook: Entity): DataContext = DataContext.fishing(fishingHook)

    @JvmStatic
    fun fishing(
        fishingHook: Entity,
        support: VanillaLootSupport,
        tool: ItemStack,
        origin: Point,
        entityTargets: Map<String, Entity>,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.fishing(
        fishingHook,
        support,
        tool,
        origin,
        entityTargets,
        randomSequences,
        lootLuck,
        structureMatcher,
    )

    @JvmStatic
    fun blockInteract(instance: Instance, block: Block, blockPosition: Point): DataContext =
        DataContext.blockInteract(instance, block, blockPosition)

    @JvmStatic
    fun blockInteract(
        instance: Instance,
        block: Block,
        blockPosition: Point,
        support: VanillaLootSupport,
        interactingEntity: Entity?,
        tool: ItemStack,
        entityTargets: Map<String, Entity>,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.blockInteract(
        instance,
        block,
        blockPosition,
        support,
        interactingEntity,
        tool,
        entityTargets,
        randomSequences,
        lootLuck,
        structureMatcher,
    )

    @JvmStatic
    fun blockInteract(event: PlayerBlockInteractEvent): DataContext = DataContext.blockInteract(event)

    @JvmStatic
    fun blockInteract(
        event: PlayerBlockInteractEvent,
        support: VanillaLootSupport,
        randomSequences: RandomSequenceController,
        lootLuck: Float,
        structureMatcher: StructurePredicateMatcher?,
    ): DataContext = DataContext.blockInteract(
        event,
        support,
        randomSequences,
        lootLuck,
        structureMatcher,
    )
}
