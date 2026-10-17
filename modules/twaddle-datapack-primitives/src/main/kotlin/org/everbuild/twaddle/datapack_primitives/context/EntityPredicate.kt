package org.everbuild.twaddle.datapack_primitives.context

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.component.DataComponent
import net.minestom.server.component.DataComponents
import net.minestom.server.registry.DynamicRegistry
import net.minestom.server.registry.Registry

/** One value in the identifier-keyed entity-predicate map. */
sealed interface EntityPredicateValue {
    val key: Key

    data class Component(
        override val key: Key,
        val component: DataComponent<*>,
        val value: Any,
    ) : EntityPredicateValue

    data class SubPredicate(
        override val key: Key,
        val value: EntityPredicateSubPredicate,
    ) : EntityPredicateValue

    data class Raw(
        override val key: Key,
        val value: Codec.RawValue,
    ) : EntityPredicateValue
}

/** Decoded 26.2+ entity-predicate entries, keyed by component or sub-predicate ID. */
data class EntityPredicate(val subPredicates: Map<Key, EntityPredicateValue>)

/** Payload type for a registered entity sub-predicate ID. */
interface EntityPredicateSubPredicate

/** Provides the codec for one entity sub-predicate ID. */
fun interface EntityPredicateSubPredicateType {
    fun codec(): Codec<EntityPredicateSubPredicate>

    fun codec(entityPredicateCodec: Codec<EntityPredicate>): Codec<EntityPredicateSubPredicate> = codec()
}

/** Creates a map codec that selects an entry codec from the component or sub-predicate registry. */
fun createEntityPredicateCodec(registry: Registry<EntityPredicateSubPredicateType>): Codec<EntityPredicate> =
    Codec.Recursive { entityPredicateCodec ->
        Codec.KEY.mapValueTyped({ key ->
            val subPredicateType = registry.get(key)
            if (subPredicateType != null) {
                subPredicateValueCodec(key, subPredicateType, entityPredicateCodec)
            } else {
                componentValueCodec(key) ?: rawValueCodec(key)
            }
        }).transform(::EntityPredicate, EntityPredicate::subPredicates)
    }

internal val rawEntityPredicateCodec: Codec<EntityPredicate> = Codec.KEY.mapValue(Codec.RAW_VALUE).transform(
    { values -> EntityPredicate(values.mapValues { (key, value) -> EntityPredicateValue.Raw(key, value) }) },
    { predicate ->
        predicate.subPredicates.mapValues { (key, value) ->
            require(value is EntityPredicateValue.Raw && value.key == key) {
                "Expected raw entity predicate value for $key, got $value"
            }
            value.value
        }
    },
)

@Suppress("UNCHECKED_CAST")
private fun componentValueCodec(key: Key): Codec<EntityPredicateValue>? {
    DataComponents.CUSTOM_NAME // Trigger Minestom's built-in component registrations before lookup.
    val component = DataComponent.fromKey(key) ?: return null
    val componentCodec = (component as DataComponent<Any>).codec() ?: return null
    return componentCodec.transform(
        { value -> EntityPredicateValue.Component(key, component, value) },
        { value ->
            require(value is EntityPredicateValue.Component && value.key == key) {
                "Expected entity component value for $key, got $value"
            }
            value.value
        },
    )
}

private fun subPredicateValueCodec(
    key: Key,
    type: EntityPredicateSubPredicateType,
    entityPredicateCodec: Codec<EntityPredicate>,
): Codec<EntityPredicateValue> = type.codec(entityPredicateCodec).transform(
    { value -> EntityPredicateValue.SubPredicate(key, value) },
    { value ->
        require(value is EntityPredicateValue.SubPredicate && value.key == key) {
            "Expected entity sub-predicate value for $key, got $value"
        }
        value.value
    },
)

private fun rawValueCodec(key: Key): Codec<EntityPredicateValue> = Codec.RAW_VALUE.transform(
    { value -> EntityPredicateValue.Raw(key, value) },
    { value ->
        require(value is EntityPredicateValue.Raw && value.key == key) {
            "Expected raw entity predicate value for $key, got $value"
        }
        value.value
    },
)

/** Adds built-in entity sub-predicate codecs to the supplied registry. */
fun addAllVanillaEntityPredicateSubPredicates(registry: DynamicRegistry<EntityPredicateSubPredicateType>) {
    registry.register(Key.key("minecraft:entity_type"), EntityTypePredicateType)
    registry.register(Key.key("minecraft:location"), LocationPredicateSubType)
    registry.register(Key.key("minecraft:stepping_on"), LocationPredicateSubType)
    registry.register(Key.key("minecraft:team"), TeamPredicateType)
    registry.register(Key.key("minecraft:periodic_tick"), PeriodicTickPredicateType)
    registry.register(Key.key("minecraft:entity_tags"), EntityTagsPredicateType)
    registry.register(Key.key("minecraft:distance"), EntityDistancePredicateType)
    registry.register(Key.key("minecraft:flags"), EntityFlagsPredicateType)
    registry.register(Key.key("minecraft:effects"), EntityEffectsPredicateType)
    registry.register(Key.key("minecraft:movement"), EntityMovementPredicateType)
    registry.register(Key.key("minecraft:passengers"), EntityPassengersPredicateType)
    registry.register(Key.key("minecraft:targeted_entity"), EntityNestedPredicateType)
    registry.register(Key.key("minecraft:vehicle"), EntityNestedPredicateType)
    registry.register(Key.key("minecraft:equipment"), EntityEquipmentPredicateType)
    registry.register(Key.key("minecraft:slots"), EntitySlotsPredicateType)
    registry.register(Key.key("minecraft:type_specific/fishing_hook"), FishingHookPredicateType)
    registry.register(Key.key("minecraft:type_specific/lightning"), LightningPredicateType)
    registry.register(Key.key("minecraft:type_specific/raider"), RaiderPredicateType)
    registry.register(Key.key("minecraft:type_specific/sheep"), SheepPredicateType)
    registry.register(Key.key("minecraft:type_specific/slime"), SlimePredicateType)
}
