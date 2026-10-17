package org.everbuild.twaddle.datapack_primitives.predicate

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicate

/** Supplies the codec for one registered context-predicate type. */
interface ContextPredicateType {
    /** Builds this type's codec, with codecs for nested predicates and number providers. */
    fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate>

    /** Builds this type's codec with the configured codec for nested entity predicates. */
    fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
        entityPredicateCodec: Codec<EntityPredicate>,
    ): StructCodec<out ContextPredicate> = codec(predicateCodec, intProviderCodec, floatProviderCodec)
}

internal object ContextPredicateKeys {
    val ALL_OF = Key.key("minecraft:all_of")
    val ANY_OF = Key.key("minecraft:any_of")
    val INVERTED = Key.key("minecraft:inverted")
    val RANDOM_CHANCE = Key.key("minecraft:random_chance")
    val INT_VALUE_CHECK = Key.key("minecraft:int_value_check")
    val FLOAT_VALUE_CHECK = Key.key("minecraft:float_value_check")
    val TIME_CHECK = Key.key("minecraft:time_check")
    val WEATHER_CHECK = Key.key("minecraft:weather_check")
    val LOCATION_CHECK = Key.key("minecraft:location_check")
    val ENVIRONMENT_ATTRIBUTE_CHECK = Key.key("minecraft:environment_attribute_check")
    val KILLED_BY_PLAYER = Key.key("minecraft:killed_by_player")
    val ENTITY_PROPERTIES = Key.key("minecraft:entity_properties")
    val ENTITY_SCORES = Key.key("minecraft:entity_scores")
    val DAMAGE_SOURCE_PROPERTIES = Key.key("minecraft:damage_source_properties")
    val MATCH_TOOL = Key.key("minecraft:match_tool")
    val MATCH_BLOCK = Key.key("minecraft:match_block")
    val TABLE_BONUS = Key.key("minecraft:table_bonus")
    val RANDOM_CHANCE_WITH_ENCHANTED_BONUS = Key.key("minecraft:random_chance_with_enchanted_bonus")
    val SURVIVES_EXPLOSION = Key.key("minecraft:survives_explosion")
    val ENCHANTMENT_ACTIVE_CHECK = Key.key("minecraft:enchantment_active_check")
}
