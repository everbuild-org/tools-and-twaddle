package org.everbuild.twaddle.datapack_primitives.number_provider

import net.minestom.server.codec.Codec
import net.minestom.server.registry.DynamicRegistry
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.AbsNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.AddNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.AverageNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.BinomialNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.CeilNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.ConditionalNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.ConstantNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.CosNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.DivideNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.EnchantmentLevelNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.EnvironmentAttributeNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.FloorNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.FloorDivideNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.FloorModNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.FromFloatNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.FromIntNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.LengthNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.MaximumNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.MinimumNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.ModNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.MultiplyNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.NegateNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.NumberDispatcherNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.PowerNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.RoundNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.ScoreNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.SinNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.SqrtNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.StorageNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.SubtractNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.TruncateNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.UniformNumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.WeightedListNumberProviderType
import kotlin.math.abs
import kotlin.math.pow

val INT_NUMBER_PROVIDER_ARITHMETIC = NumberProviderArithmetic(
    valueCodec = Codec.INT,
    zero = 0,
    fromInt = Int::toInt,
    add = Int::plus,
    abs = ::abs,
    divide = Int::div,
    binomial = { n, p, random ->
        require(p in 0.0..1.0) {
            "Probability must be between 0 and 1, got $p"
        }
        var successes = 0
        repeat(n.coerceAtLeast(0)) {
            if (random.nextFloat() < p) successes++
        }
        successes
    },
    multiply = Int::times,
    negate = Int::unaryMinus,
    min = Int::coerceAtMost,
    max = Int::coerceAtLeast,
    mod = Int::rem,
    floorDivide = { left, right -> Math.floorDiv(left, right) },
    floorMod = { left, right -> Math.floorMod(left, right) },
    pow = { base, exponent -> base.toDouble().pow(exponent.toDouble()).toInt() },
    fromFloat = Float::toInt,
)

val FLOAT_NUMBER_PROVIDER_ARITHMETIC = NumberProviderArithmetic(
    valueCodec = Codec.FLOAT,
    zero = 0f,
    fromInt = Int::toFloat,
    add = Float::plus,
    abs = ::abs,
    divide = Float::div,
    multiply = Float::times,
    negate = Float::unaryMinus,
    min = Float::coerceAtMost,
    max = Float::coerceAtLeast,
    mod = Float::rem,
    floorDivide = { left, right -> kotlin.math.floor(left / right) },
    floorMod = { left, right -> left - kotlin.math.floor(left / right) * right },
    pow = { base, exponent -> base.toDouble().pow(exponent.toDouble()).toFloat() },
    fromFloat = Float::toFloat,
)

/** Registers the implemented vanilla provider types for this value arithmetic. */
fun <T> addAllVanillaNumberProviders(
    registry: DynamicRegistry<NumberProviderType<T>>,
    arithmetic: NumberProviderArithmetic<T>,
) {
    registry.register(NumberProviderKeys.ABS, AbsNumberProviderType())
    registry.register(NumberProviderKeys.ADD, AddNumberProviderType())
    registry.register(NumberProviderKeys.AVG, AverageNumberProviderType())
    registry.register(NumberProviderKeys.CONDITIONAL, ConditionalNumberProviderType())
    registry.register(NumberProviderKeys.CONSTANT, ConstantNumberProviderType())
    registry.register(NumberProviderKeys.DIV, DivideNumberProviderType())
    registry.register(NumberProviderKeys.ENVIRONMENT_ATTRIBUTE, EnvironmentAttributeNumberProviderType())
    registry.register(NumberProviderKeys.MAX, MaximumNumberProviderType())
    registry.register(NumberProviderKeys.MIN, MinimumNumberProviderType())
    registry.register(NumberProviderKeys.MOD, ModNumberProviderType())
    registry.register(NumberProviderKeys.MUL, MultiplyNumberProviderType())
    registry.register(NumberProviderKeys.NEGATE, NegateNumberProviderType())
    registry.register(NumberProviderKeys.NUMBER_DISPATCHER, NumberDispatcherNumberProviderType())
    registry.register(NumberProviderKeys.POW, PowerNumberProviderType())
    registry.register(NumberProviderKeys.STORAGE, StorageNumberProviderType())
    registry.register(NumberProviderKeys.SUB, SubtractNumberProviderType())
    registry.register(NumberProviderKeys.UNIFORM, UniformNumberProviderType())
    registry.register(NumberProviderKeys.WEIGHTED_LIST, WeightedListNumberProviderType())
    if (arithmetic.valueCodec == Codec.INT) {
        registry.register(NumberProviderKeys.FLOOR_DIV, FloorDivideNumberProviderType())
        registry.register(NumberProviderKeys.FLOOR_MOD, FloorModNumberProviderType())
        registry.register(NumberProviderKeys.FROM_FLOAT, FromFloatNumberProviderType())
        registry.register(NumberProviderKeys.SCORE, ScoreNumberProviderType())
    }
    if (arithmetic.binomial != null) {
        registry.register(NumberProviderKeys.BINOMIAL, BinomialNumberProviderType())
    }
    if (arithmetic.valueCodec == Codec.FLOAT) {
        @Suppress("UNCHECKED_CAST")
        val floatRegistry = registry as DynamicRegistry<NumberProviderType<Float>>
        floatRegistry.register(NumberProviderKeys.CEIL, CeilNumberProviderType())
        floatRegistry.register(NumberProviderKeys.COS, CosNumberProviderType())
        floatRegistry.register(NumberProviderKeys.ENCHANTMENT_LEVEL, EnchantmentLevelNumberProviderType())
        floatRegistry.register(NumberProviderKeys.FLOOR, FloorNumberProviderType())
        floatRegistry.register(NumberProviderKeys.FROM_INT, FromIntNumberProviderType())
        floatRegistry.register(NumberProviderKeys.LENGTH, LengthNumberProviderType())
        floatRegistry.register(NumberProviderKeys.ROUND, RoundNumberProviderType())
        floatRegistry.register(NumberProviderKeys.SIN, SinNumberProviderType())
        floatRegistry.register(NumberProviderKeys.SQRT, SqrtNumberProviderType())
        floatRegistry.register(NumberProviderKeys.TRUNCATE, TruncateNumberProviderType())
    }
}
