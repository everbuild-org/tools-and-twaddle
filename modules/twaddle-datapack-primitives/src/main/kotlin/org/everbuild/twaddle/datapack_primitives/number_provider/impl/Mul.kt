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

private data class MultiplyDeclaration<T>(val inputs: List<NumberProviderDeclaration<T>>, val arithmetic: NumberProviderArithmetic<T>) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.MUL
    override fun get(context: NumberProviderContext, random: Random): T = inputs.fold(arithmetic.fromInt(1)) { result, input -> arithmetic.multiply(result, input.get(context, random)) }
}

internal class MultiplyNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<NumberProviderCondition>, arithmetic: NumberProviderArithmetic<T>) = StructCodec.struct(
        "inputs", providerCodec.list(), { (it as MultiplyDeclaration<T>).inputs },
        { inputs -> MultiplyDeclaration(inputs, arithmetic) },
    ).asProviderCodec()
}
