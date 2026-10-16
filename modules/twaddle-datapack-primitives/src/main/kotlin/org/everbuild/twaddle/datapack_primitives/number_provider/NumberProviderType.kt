package org.everbuild.twaddle.datapack_primitives.number_provider

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec

internal object NumberProviderKeys {
    val ABS = Key.key("minecraft:abs")
    val ADD = Key.key("minecraft:add")
    val AVG = Key.key("minecraft:avg")
    val BINOMIAL = Key.key("minecraft:binomial")
    val CEIL = Key.key("minecraft:ceil")
    val CONDITIONAL = Key.key("minecraft:conditional")
    val CONSTANT = Key.key("minecraft:constant")
    val COS = Key.key("minecraft:cos")
    val DIV = Key.key("minecraft:div")
    val ENCHANTMENT_LEVEL = Key.key("minecraft:enchantment_level")
    val ENVIRONMENT_ATTRIBUTE = Key.key("minecraft:environment_attribute")
    val FLOOR = Key.key("minecraft:floor")
    val FLOOR_DIV = Key.key("minecraft:floor_div")
    val FLOOR_MOD = Key.key("minecraft:floor_mod")
    val FROM_FLOAT = Key.key("minecraft:from_float")
    val FROM_INT = Key.key("minecraft:from_int")
    val LENGTH = Key.key("minecraft:length")
    val MAX = Key.key("minecraft:max")
    val MIN = Key.key("minecraft:min")
    val MOD = Key.key("minecraft:mod")
    val MUL = Key.key("minecraft:mul")
    val NEGATE = Key.key("minecraft:negate")
    val NUMBER_DISPATCHER = Key.key("minecraft:number_dispatcher")
    val POW = Key.key("minecraft:pow")
    val ROUND = Key.key("minecraft:round")
    val SCORE = Key.key("minecraft:score")
    val SIN = Key.key("minecraft:sin")
    val SQRT = Key.key("minecraft:sqrt")
    val STORAGE = Key.key("minecraft:storage")
    val SUB = Key.key("minecraft:sub")
    val TRUNCATE = Key.key("minecraft:truncate")
    val UNIFORM = Key.key("minecraft:uniform")
    val WEIGHTED_LIST = Key.key("minecraft:weighted_list")
}

interface NumberProviderType<T> {
    fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<NumberProviderCondition>,
        arithmetic: NumberProviderArithmetic<T>,
    ): StructCodec<NumberProviderDeclaration<T>>
}
