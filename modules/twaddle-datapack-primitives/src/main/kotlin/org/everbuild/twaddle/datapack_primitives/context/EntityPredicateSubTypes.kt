package org.everbuild.twaddle.datapack_primitives.context

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec

data class EntityTypePredicate(val types: RegistryEntrySet) : EntityPredicateSubPredicate

internal object EntityTypePredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = registryEntrySetCodec.transform(
        ::EntityTypePredicate,
    ) { (it as EntityTypePredicate).types }
}

data class LocationEntityPredicate(val location: LocationPredicate) : EntityPredicateSubPredicate

internal object LocationPredicateSubType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = LOCATION_PREDICATE_CODEC.transform(
        ::LocationEntityPredicate,
    ) { (it as LocationEntityPredicate).location }
}

data class TeamPredicate(val team: String) : EntityPredicateSubPredicate

internal object TeamPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = Codec.STRING.transform(
        ::TeamPredicate,
    ) { (it as TeamPredicate).team }
}

data class PeriodicTickPredicate(val period: Int) : EntityPredicateSubPredicate

internal object PeriodicTickPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = Codec.INT.transform(
        ::PeriodicTickPredicate,
    ) { (it as PeriodicTickPredicate).period }
}

data class EntityTagsPredicate(
    val anyOf: List<String>?,
    val allOf: List<String>?,
    val noneOf: List<String>?,
) : EntityPredicateSubPredicate

internal object EntityTagsPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "any_of", Codec.STRING.list().optional(), EntityTagsPredicate::anyOf,
        "all_of", Codec.STRING.list().optional(), EntityTagsPredicate::allOf,
        "none_of", Codec.STRING.list().optional(), EntityTagsPredicate::noneOf,
        ::EntityTagsPredicate,
    ).transform({ it }, { it as EntityTagsPredicate })
}

data class EntityDistancePredicate(
    val x: LocationRange<Double>?,
    val y: LocationRange<Double>?,
    val z: LocationRange<Double>?,
    val horizontal: LocationRange<Double>?,
    val absolute: LocationRange<Double>?,
) : EntityPredicateSubPredicate

internal object EntityDistancePredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "x", locationRangeCodec(Codec.DOUBLE).optional(), EntityDistancePredicate::x,
        "y", locationRangeCodec(Codec.DOUBLE).optional(), EntityDistancePredicate::y,
        "z", locationRangeCodec(Codec.DOUBLE).optional(), EntityDistancePredicate::z,
        "horizontal", locationRangeCodec(Codec.DOUBLE).optional(), EntityDistancePredicate::horizontal,
        "absolute", locationRangeCodec(Codec.DOUBLE).optional(), EntityDistancePredicate::absolute,
        ::EntityDistancePredicate,
    ).transform({ it }, { it as EntityDistancePredicate })
}

data class EntityFlagsPredicate(
    val isBaby: Boolean?,
    val isOnFire: Boolean?,
    val isSneaking: Boolean?,
    val isSprinting: Boolean?,
    val isSwimming: Boolean?,
    val isOnGround: Boolean?,
    val isFlying: Boolean?,
    val isGliding: Boolean?,
) : EntityPredicateSubPredicate

internal object EntityFlagsPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "is_baby", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isBaby,
        "is_on_fire", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isOnFire,
        "is_sneaking", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isSneaking,
        "is_sprinting", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isSprinting,
        "is_swimming", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isSwimming,
        "is_on_ground", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isOnGround,
        "is_flying", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isFlying,
        "is_gliding", Codec.BOOLEAN.optional(), EntityFlagsPredicate::isGliding,
        ::EntityFlagsPredicate,
    ).transform({ it }, { it as EntityFlagsPredicate })
}

data class EntityEffectPredicate(
    val amplifier: LocationRange<Int>?,
    val duration: LocationRange<Int>?,
    val ambient: Boolean?,
    val visible: Boolean?,
    val showParticles: Boolean?,
)

data class EntityEffectsPredicate(val effects: Map<Key, EntityEffectPredicate>) : EntityPredicateSubPredicate

private val entityEffectPredicateCodec = StructCodec.struct(
    "amplifier", locationRangeCodec(Codec.INT).optional(), EntityEffectPredicate::amplifier,
    "duration", locationRangeCodec(Codec.INT).optional(), EntityEffectPredicate::duration,
    "ambient", Codec.BOOLEAN.optional(), EntityEffectPredicate::ambient,
    "visible", Codec.BOOLEAN.optional(), EntityEffectPredicate::visible,
    "show_particles", Codec.BOOLEAN.optional(), EntityEffectPredicate::showParticles,
    ::EntityEffectPredicate,
)

internal object EntityEffectsPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = Codec.KEY.mapValue(entityEffectPredicateCodec)
        .transform(::EntityEffectsPredicate) { (it as EntityEffectsPredicate).effects }
}

data class EntityMovementPredicate(
    val x: LocationRange<Double>?,
    val y: LocationRange<Double>?,
    val z: LocationRange<Double>?,
    val speed: LocationRange<Double>?,
    val horizontalSpeed: LocationRange<Double>?,
    val verticalSpeed: LocationRange<Double>?,
    val fallDistance: LocationRange<Double>?,
) : EntityPredicateSubPredicate

internal object EntityMovementPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "x", locationRangeCodec(Codec.DOUBLE).optional(), EntityMovementPredicate::x,
        "y", locationRangeCodec(Codec.DOUBLE).optional(), EntityMovementPredicate::y,
        "z", locationRangeCodec(Codec.DOUBLE).optional(), EntityMovementPredicate::z,
        "speed", locationRangeCodec(Codec.DOUBLE).optional(), EntityMovementPredicate::speed,
        "horizontal_speed", locationRangeCodec(Codec.DOUBLE).optional(), EntityMovementPredicate::horizontalSpeed,
        "vertical_speed", locationRangeCodec(Codec.DOUBLE).optional(), EntityMovementPredicate::verticalSpeed,
        "fall_distance", locationRangeCodec(Codec.DOUBLE).optional(), EntityMovementPredicate::fallDistance,
        ::EntityMovementPredicate,
    ).transform({ it }, { it as EntityMovementPredicate })
}

data class EntityPassengersPredicate(val passengers: List<EntityPredicate>) : EntityPredicateSubPredicate
data class EntityNestedPredicate(val predicate: EntityPredicate) : EntityPredicateSubPredicate

internal object EntityPassengersPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = error("Requires recursive entity-predicate codec")

    override fun codec(entityPredicateCodec: Codec<EntityPredicate>): Codec<EntityPredicateSubPredicate> =
        entityPredicateCodec.list().transform(
            ::EntityPassengersPredicate,
        ) { (it as EntityPassengersPredicate).passengers }
}

internal object EntityNestedPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = error("Requires recursive entity-predicate codec")

    override fun codec(entityPredicateCodec: Codec<EntityPredicate>): Codec<EntityPredicateSubPredicate> =
        entityPredicateCodec.transform(
            ::EntityNestedPredicate,
        ) { (it as EntityNestedPredicate).predicate }
}

data class EntityEquipmentPredicate(val equipment: Map<String, Codec.RawValue>) : EntityPredicateSubPredicate
data class EntitySlotsPredicate(val slots: Map<String, Codec.RawValue>) : EntityPredicateSubPredicate

internal object EntityEquipmentPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = Codec.STRING.mapValue(Codec.RAW_VALUE)
        .transform(::EntityEquipmentPredicate) { (it as EntityEquipmentPredicate).equipment }
}

internal object EntitySlotsPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = Codec.STRING.mapValue(Codec.RAW_VALUE)
        .transform(::EntitySlotsPredicate) { (it as EntitySlotsPredicate).slots }
}

data class FishingHookPredicate(val inOpenWater: Boolean?) : EntityPredicateSubPredicate

internal object FishingHookPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "in_open_water", Codec.BOOLEAN.optional(), FishingHookPredicate::inOpenWater,
        ::FishingHookPredicate,
    ).transform({ it }, { it as FishingHookPredicate })
}

data class RaiderPredicate(val isCaptain: Boolean?, val hasRaid: Boolean?) : EntityPredicateSubPredicate

internal object RaiderPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "is_captain", Codec.BOOLEAN.optional(), RaiderPredicate::isCaptain,
        "has_raid", Codec.BOOLEAN.optional(), RaiderPredicate::hasRaid,
        ::RaiderPredicate,
    ).transform({ it }, { it as RaiderPredicate })
}

data class SheepPredicate(val sheared: Boolean?) : EntityPredicateSubPredicate

internal object SheepPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "sheared", Codec.BOOLEAN.optional(), SheepPredicate::sheared,
        ::SheepPredicate,
    ).transform({ it }, { it as SheepPredicate })
}

data class SlimePredicate(val size: LocationRange<Int>?) : EntityPredicateSubPredicate

internal object SlimePredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "size", locationRangeCodec(Codec.INT).optional(), SlimePredicate::size,
        ::SlimePredicate,
    ).transform({ it }, { it as SlimePredicate })
}

data class LightningPredicate(
    val blocksSetOnFire: LocationRange<Int>?,
    val entityStruck: EntityPredicate?,
) : EntityPredicateSubPredicate

internal object LightningPredicateType : EntityPredicateSubPredicateType {
    override fun codec(): Codec<EntityPredicateSubPredicate> = error("Requires recursive entity-predicate codec")

    override fun codec(entityPredicateCodec: Codec<EntityPredicate>): Codec<EntityPredicateSubPredicate> = StructCodec.struct(
        "blocks_set_on_fire", locationRangeCodec(Codec.INT).optional(), LightningPredicate::blocksSetOnFire,
        "entity_struck", entityPredicateCodec.optional(), LightningPredicate::entityStruck,
        ::LightningPredicate,
    ).transform({ it }, { it as LightningPredicate })
}
