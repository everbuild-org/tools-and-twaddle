package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderCondition
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderKeys
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.context.NumberProviderContext
import java.util.Random

private data class UniformDeclaration<T>(
    val min: NumberProviderDeclaration<T>,
    val max: NumberProviderDeclaration<T>,
    val arithmetic: NumberProviderArithmetic<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.UNIFORM

    @Suppress("UNCHECKED_CAST")
    override fun get(context: NumberProviderContext, random: Random): T {
        val minimum = min.get(context, random)
        val maximum = max.get(context, random)
        return if (arithmetic.valueCodec == Codec.INT) {
            random.nextLong((minimum as Int).toLong(), (maximum as Int).toLong() + 1L).toInt() as T
        } else {
            val minimumFloat = minimum as Float
            val maximumFloat = maximum as Float
            (minimumFloat + random.nextFloat() * (maximumFloat - minimumFloat)) as T
        }
    }
}

internal class UniformNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<NumberProviderCondition>, arithmetic: NumberProviderArithmetic<T>) = StructCodec.struct(
        "min", providerCodec, { (it as UniformDeclaration<T>).min },
        "max", providerCodec, { it.max },
        { min, max -> UniformDeclaration(min, max, arithmetic) },
    ).asProviderCodec()
}
