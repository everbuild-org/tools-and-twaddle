package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderKeys
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

private data class AverageDeclaration<T>(
    val inputs: List<NumberProviderDeclaration<T>>,
    val arithmetic: NumberProviderArithmetic<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.AVG
    override fun get(context: DataContext, random: Random): T {
        if (inputs.isEmpty()) return arithmetic.zero
        val sum = inputs.fold(arithmetic.zero) { result, input -> arithmetic.add(result, input.get(context, random)) }
        return arithmetic.divide(sum, arithmetic.fromInt(inputs.size))
    }
}

internal class AverageNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<ContextPredicate>,
        arithmetic: NumberProviderArithmetic<T>,
    ) = StructCodec.struct(
        "inputs",
        providerCodec.list(),
        { (it as AverageDeclaration<T>).inputs },
        { AverageDeclaration(it, arithmetic) },
    ).asProviderCodec()
}
