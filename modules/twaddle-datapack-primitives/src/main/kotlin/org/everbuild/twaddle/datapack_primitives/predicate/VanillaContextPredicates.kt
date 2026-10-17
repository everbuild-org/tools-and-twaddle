package org.everbuild.twaddle.datapack_primitives.predicate

import net.kyori.adventure.key.Key
import net.minestom.server.registry.DynamicRegistry
import org.everbuild.twaddle.datapack_primitives.predicate.impl.AllOfPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.AnyOfPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.DamageSourcePropertiesPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.EnchantmentActiveCheckPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.EntityPropertiesPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.EntityScoresPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.EnvironmentAttributeCheckPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.FloatValueCheckPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.IntValueCheckPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.InvertedPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.KilledByPlayerPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.LocationCheckPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.MatchBlockPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.MatchToolPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.RandomChanceWithEnchantedBonusPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.RandomChancePredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.SurvivesExplosionPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.TableBonusPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.TimeCheckPredicateType
import org.everbuild.twaddle.datapack_primitives.predicate.impl.WeatherCheckPredicateType

/** Registers the implemented vanilla context-predicate types. */
fun addAllVanillaContextPredicates(registry: DynamicRegistry<ContextPredicateType>) {
    registry.register(ContextPredicateKeys.ALL_OF, AllOfPredicateType())
    registry.register(ContextPredicateKeys.ANY_OF, AnyOfPredicateType())
    registry.register(ContextPredicateKeys.INVERTED, InvertedPredicateType())
    registry.register(ContextPredicateKeys.RANDOM_CHANCE, RandomChancePredicateType())
    registry.register(ContextPredicateKeys.INT_VALUE_CHECK, IntValueCheckPredicateType())
    registry.register(ContextPredicateKeys.FLOAT_VALUE_CHECK, FloatValueCheckPredicateType())
    registry.register(ContextPredicateKeys.TIME_CHECK, TimeCheckPredicateType())
    registry.register(ContextPredicateKeys.WEATHER_CHECK, WeatherCheckPredicateType())
    registry.register(ContextPredicateKeys.LOCATION_CHECK, LocationCheckPredicateType())
    registry.register(ContextPredicateKeys.ENVIRONMENT_ATTRIBUTE_CHECK, EnvironmentAttributeCheckPredicateType())
    registry.register(ContextPredicateKeys.KILLED_BY_PLAYER, KilledByPlayerPredicateType())
    registry.register(ContextPredicateKeys.ENTITY_PROPERTIES, EntityPropertiesPredicateType())
    registry.register(ContextPredicateKeys.ENTITY_SCORES, EntityScoresPredicateType())
    registry.register(ContextPredicateKeys.DAMAGE_SOURCE_PROPERTIES, DamageSourcePropertiesPredicateType())
    registry.register(ContextPredicateKeys.MATCH_TOOL, MatchToolPredicateType())
    registry.register(ContextPredicateKeys.MATCH_BLOCK, MatchBlockPredicateType())
    registry.register(ContextPredicateKeys.TABLE_BONUS, TableBonusPredicateType())
    registry.register(
        ContextPredicateKeys.RANDOM_CHANCE_WITH_ENCHANTED_BONUS,
        RandomChanceWithEnchantedBonusPredicateType(),
    )
    registry.register(ContextPredicateKeys.SURVIVES_EXPLOSION, SurvivesExplosionPredicateType())
    registry.register(ContextPredicateKeys.ENCHANTMENT_ACTIVE_CHECK, EnchantmentActiveCheckPredicateType())
}

@Suppress("UnstableApiUsage")
fun createVanillaContextPredicateRegistry(): DynamicRegistry<ContextPredicateType> =
    DynamicRegistry.create<ContextPredicateType>(Key.key("twaddle:vanilla_context_predicates")).also(
        ::addAllVanillaContextPredicates,
    )
