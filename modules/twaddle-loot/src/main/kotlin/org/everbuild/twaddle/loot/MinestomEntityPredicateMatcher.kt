package org.everbuild.twaddle.loot

import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.BinaryTagTypes
import net.kyori.adventure.nbt.StringBinaryTag
import net.minestom.server.MinecraftServer
import net.minestom.server.component.DataComponents
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityCreature
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.Player
import net.minestom.server.entity.metadata.AgeableMobMeta
import net.minestom.server.entity.metadata.animal.SheepMeta
import net.minestom.server.entity.metadata.cube.AbstractCubeMeta
import net.minestom.server.entity.metadata.other.FishingHookMeta
import net.minestom.server.entity.metadata.other.LightningBoltMeta
import net.minestom.server.entity.metadata.monster.raider.RaiderMeta
import net.minestom.server.inventory.EquipmentHandler
import net.minestom.server.registry.Registries
import net.minestom.server.registry.Registry
import net.minestom.server.registry.RegistryKey
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.EntityDistancePredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityEffectsPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityEquipmentPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityFlagsPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityMovementPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityNestedPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityPassengersPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateSubPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateValue
import org.everbuild.twaddle.datapack_primitives.context.EntitySlotsPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityTagsPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityTypePredicate
import org.everbuild.twaddle.datapack_primitives.context.FishingHookPredicate
import org.everbuild.twaddle.datapack_primitives.context.ItemPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.LightningPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationContext
import org.everbuild.twaddle.datapack_primitives.context.LocationEntityPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationRange
import org.everbuild.twaddle.datapack_primitives.context.PeriodicTickPredicate
import org.everbuild.twaddle.datapack_primitives.context.RaiderPredicate
import org.everbuild.twaddle.datapack_primitives.context.RegistryEntrySet
import org.everbuild.twaddle.datapack_primitives.context.SheepPredicate
import org.everbuild.twaddle.datapack_primitives.context.SlimePredicate
import org.everbuild.twaddle.datapack_primitives.context.TeamPredicate
import kotlin.math.abs
import kotlin.math.sqrt

internal class MinestomEntityPredicateMatcher(
    private val entities: Map<String, Entity>,
    private val origin: ContextLocation,
    private val registries: Registries,
    private val locationContext: LocationContext,
    private val itemPredicateMatcher: ItemPredicateMatcher,
) : EntityPredicateMatcher {
    override fun matches(target: String, predicate: EntityPredicate): Boolean {
        val entity = entities[target] ?: return false
        return matchesEntity(entity, predicate)
    }

    private fun matchesEntity(entity: Entity, predicate: EntityPredicate): Boolean =
        predicate.subPredicates.values.all { value ->
            when (value) {
                is EntityPredicateValue.Component -> entity.get(value.component) == value.value
                is EntityPredicateValue.SubPredicate -> matchesSubPredicate(entity, value.key.asString(), value.value)
                is EntityPredicateValue.Raw -> error("Cannot evaluate raw entity predicate '${value.key}'")
            }
        }

    private fun matchesSubPredicate(entity: Entity, key: String, predicate: EntityPredicateSubPredicate): Boolean =
        when (predicate) {
            is EntityTypePredicate -> matchesEntityTypes(entity, predicate.types)
            is LocationEntityPredicate -> locationContext.evaluator.matches(entity.location(), predicate.location)
            is TeamPredicate -> matchesTeam(entity, predicate.team)
            is PeriodicTickPredicate -> predicate.period > 0 &&
                entity.aliveTicks.rem(predicate.period.toLong()) == 0L
            is EntityTagsPredicate -> matchesTags(entity, predicate)
            is EntityDistancePredicate -> matchesDistance(entity, predicate)
            is EntityFlagsPredicate -> matchesFlags(entity, predicate)
            is EntityEffectsPredicate -> matchesEffects(entity, predicate)
            is EntityMovementPredicate -> matchesMovement(entity, predicate)
            is EntityPassengersPredicate -> matchesPassengers(entity, predicate)
            is EntityNestedPredicate -> when (key) {
                "minecraft:vehicle" -> entity.vehicle?.let { matchesEntity(it, predicate.predicate) } ?: false
                "minecraft:targeted_entity" -> (entity as? EntityCreature)?.target
                    ?.let { matchesEntity(it, predicate.predicate) } ?: false
                else -> unsupported(entity, key)
            }
            is EntityEquipmentPredicate -> matchesEquipment(entity, predicate.equipment)
            is EntitySlotsPredicate -> matchesEquipment(entity, predicate.slots)
            is FishingHookPredicate -> matchesFishingHook(entity, predicate)
            is RaiderPredicate -> matchesRaider(entity, predicate)
            is SheepPredicate -> matchesSheep(entity, predicate)
            is SlimePredicate -> matchesSlime(entity, predicate)
            is LightningPredicate -> matchesLightning(entity, predicate)
            else -> error("No vanilla Minestom evaluator for entity sub-predicate '$key' (${predicate::class.simpleName})")
        }

    private fun matchesEntityTypes(entity: Entity, types: RegistryEntrySet): Boolean {
        val key = registries.entityType().getKey(entity.entityType) ?: return false
        return matchesEntrySet(types, registries.entityType(), key)
    }

    private fun matchesTeam(entity: Entity, teamName: String): Boolean {
        val team = MinecraftServer.getTeamManager().getTeam(teamName) ?: return false
        val member = if (entity is Player) entity.username else entity.uuid.toString()
        return member in team.members
    }

    private fun matchesTags(entity: Entity, predicate: EntityTagsPredicate): Boolean {
        val customData = entity.get(DataComponents.CUSTOM_DATA)
        val tags = customData?.nbt()?.getList("Tags", BinaryTagTypes.STRING)
            ?.mapNotNull { (it as? StringBinaryTag)?.value() }
            ?.toSet().orEmpty()
        val anyOf = predicate.anyOf
        val allOf = predicate.allOf
        val noneOf = predicate.noneOf
        return (anyOf == null || anyOf.any(tags::contains)) &&
            (allOf == null || allOf.all(tags::contains)) &&
            (noneOf == null || noneOf.none(tags::contains))
    }

    private fun matchesDistance(entity: Entity, predicate: EntityDistancePredicate): Boolean {
        val position = entity.position
        val dx = abs(position.x() - origin.x)
        val dy = abs(position.y() - origin.y)
        val dz = abs(position.z() - origin.z)
        return predicate.x.matchesOptional(dx) &&
            predicate.y.matchesOptional(dy) &&
            predicate.z.matchesOptional(dz) &&
            predicate.horizontal.matchesOptional(sqrt(dx * dx + dz * dz)) &&
            predicate.absolute.matchesOptional(sqrt(dx * dx + dy * dy + dz * dz))
    }

    private fun matchesFlags(entity: Entity, predicate: EntityFlagsPredicate): Boolean {
        val meta = entity.entityMeta
        val ageableMeta = meta as? AgeableMobMeta
        val isBaby = ageableMeta?.isBaby == true
        val flying = (entity as? Player)?.isFlying == true
        return predicate.isBaby.matchesExpected(isBaby) &&
            predicate.isOnFire.matchesExpected(entity.isOnFire) &&
            predicate.isSneaking.matchesExpected(entity.isSneaking) &&
            predicate.isSprinting.matchesExpected(entity.isSprinting) &&
            predicate.isSwimming.matchesExpected(meta.isSwimming) &&
            predicate.isOnGround.matchesExpected(entity.isOnGround) &&
            predicate.isFlying.matchesExpected(flying) &&
            predicate.isGliding.matchesExpected(meta.isFlyingWithElytra)
    }

    private fun matchesEffects(entity: Entity, predicate: EntityEffectsPredicate): Boolean {
        val effects = entity.activeEffects.associateBy { it.potion.effect.key() }
        return predicate.effects.all { (key, requirement) ->
            val timed = effects[key] ?: return@all false
            val potion = timed.potion
            val remainingDuration = if (potion.duration() < 0) {
                -1
            } else {
                (potion.duration().toLong() - (entity.aliveTicks - timed.startingTicks))
                    .coerceIn(0L, Int.MAX_VALUE.toLong())
                    .toInt()
            }
                requirement.amplifier.matchesOptional(potion.amplifier()) &&
                requirement.duration.matchesOptional(remainingDuration) &&
                requirement.ambient.matchesExpected(potion.isAmbient) &&
                requirement.visible.matchesExpected(potion.hasIcon()) &&
                requirement.showParticles.matchesExpected(potion.hasParticles())
        }
    }

    private fun matchesMovement(entity: Entity, predicate: EntityMovementPredicate): Boolean {
        if (predicate.fallDistance != null) {
            error("Minestom does not expose fall distance for vanilla entity predicates")
        }
        val velocity = entity.velocity
        val speed = sqrt(velocity.x() * velocity.x() + velocity.y() * velocity.y() + velocity.z() * velocity.z())
        val horizontalSpeed = sqrt(velocity.x() * velocity.x() + velocity.z() * velocity.z())
        return predicate.x.matchesOptional(velocity.x()) &&
            predicate.y.matchesOptional(velocity.y()) &&
            predicate.z.matchesOptional(velocity.z()) &&
            predicate.speed.matchesOptional(speed) &&
            predicate.horizontalSpeed.matchesOptional(horizontalSpeed) &&
            predicate.verticalSpeed.matchesOptional(abs(velocity.y()))
    }

    private fun matchesPassengers(entity: Entity, predicate: EntityPassengersPredicate): Boolean {
        val passengers = entity.passengers
        if (predicate.passengers.size > passengers.size) return false
        val used = BooleanArray(passengers.size)

        fun assign(predicateIndex: Int): Boolean {
            if (predicateIndex == predicate.passengers.size) return true
            for (passengerIndex in passengers.indices) {
                if (used[passengerIndex] || !matchesEntity(passengers[passengerIndex], predicate.passengers[predicateIndex])) continue
                used[passengerIndex] = true
                if (assign(predicateIndex + 1)) return true
                used[passengerIndex] = false
            }
            return false
        }

        return assign(0)
    }

    private fun matchesEquipment(
        entity: Entity,
        equipment: Map<String, net.minestom.server.codec.Codec.RawValue>,
    ): Boolean {
        val handler = entity as? EquipmentHandler ?: return false
        return equipment.all { (slotName, rawPredicate) ->
            val slot = slot(slotName) ?: return@all false
            itemPredicateMatcher.matches(handler.getEquipment(slot), rawPredicate)
        }
    }

    private fun matchesFishingHook(entity: Entity, predicate: FishingHookPredicate): Boolean {
        if (entity.entityMeta !is FishingHookMeta) return false
        if (predicate.inOpenWater != null) {
            error("Minestom exposes fishing-hook catchability, but not vanilla open-water state")
        }
        return true
    }

    private fun matchesRaider(entity: Entity, predicate: RaiderPredicate): Boolean {
        if (entity.entityMeta !is RaiderMeta) return false
        if (predicate.isCaptain != null || predicate.hasRaid != null) {
            error("Minestom does not expose vanilla raider captain or raid state")
        }
        return true
    }

    private fun matchesSheep(entity: Entity, predicate: SheepPredicate): Boolean {
        val meta = entity.entityMeta as? SheepMeta ?: return false
        return predicate.sheared.matchesExpected(meta.isSheared)
    }

    private fun matchesSlime(entity: Entity, predicate: SlimePredicate): Boolean {
        if (entity.entityType.key() != Key.key("minecraft:slime")) return false
        val meta = entity.entityMeta as? AbstractCubeMeta ?: return false
        return predicate.size.matchesOptional(meta.size)
    }

    private fun matchesLightning(entity: Entity, predicate: LightningPredicate): Boolean {
        if (entity.entityMeta !is LightningBoltMeta) return false
        if (predicate.blocksSetOnFire != null || predicate.entityStruck != null) {
            error("Minestom does not expose vanilla lightning-strike history")
        }
        return true
    }

    private fun unsupported(entity: Entity, predicate: String): Nothing =
        error("Unsupported entity sub-predicate '$predicate' for ${entity.entityType.key()}")

    private fun Entity.location() = ContextLocation(position.x(), position.y(), position.z())

    private fun slot(slotName: String): EquipmentSlot? = when (slotName) {
        "mainhand", "main_hand" -> EquipmentSlot.MAIN_HAND
        "offhand", "off_hand" -> EquipmentSlot.OFF_HAND
        "head", "helmet" -> EquipmentSlot.HELMET
        "chest", "chestplate" -> EquipmentSlot.CHESTPLATE
        "legs", "leggings" -> EquipmentSlot.LEGGINGS
        "feet", "boots" -> EquipmentSlot.BOOTS
        "body" -> EquipmentSlot.BODY
        "saddle" -> EquipmentSlot.SADDLE
        else -> null
    }

    private fun <T> matchesEntrySet(
        entries: RegistryEntrySet,
        registry: Registry<T>,
        value: RegistryKey<T>,
    ): Boolean = entries.values.any { reference ->
        if (reference.tag) registry.getTag(reference.key)?.contains(value) == true
        else reference.key == value.key()
    }
}

private fun <T : Comparable<T>> LocationRange<T>?.matchesOptional(value: T): Boolean =
    this == null || matches(value)

private fun <T : Comparable<T>> LocationRange<T>.matches(value: T): Boolean {
    val minimum = min
    val maximum = max
    return (minimum == null || value >= minimum) && (maximum == null || value <= maximum)
}

private fun Boolean?.matchesExpected(value: Boolean): Boolean = this == null || this == value
