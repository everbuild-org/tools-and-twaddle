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
import kotlin.math.sqrt

private data class LengthDeclaration(
    val inputs: List<NumberProviderDeclaration<Float>>,
) : NumberProviderDeclaration<Float> {
    override fun type() = NumberProviderKeys.LENGTH
    override fun get(context: NumberProviderContext, random: Random): Float {
        val squaredLength = inputs.sumOf { input ->
            val value = input.get(context, random).toDouble()
            value * value
        }
        return sqrt(squaredLength).toFloat()
    }
}

internal class LengthNumberProviderType : NumberProviderType<Float> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<Float>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<NumberProviderCondition>,
        arithmetic: NumberProviderArithmetic<Float>,
    ) = StructCodec.struct(
        "inputs", providerCodec.list(),
        { (it as LengthDeclaration).inputs },
        { inputs -> LengthDeclaration(inputs) },
    ).asProviderCodec()
}
