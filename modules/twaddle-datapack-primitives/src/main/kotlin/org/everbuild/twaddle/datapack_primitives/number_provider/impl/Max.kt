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

private data class MaximumDeclaration<T>(val inputs: List<NumberProviderDeclaration<T>>, val arithmetic: NumberProviderArithmetic<T>) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.MAX
    override fun get(context: DataContext, random: Random): T {
        val values = inputs.map { it.get(context, random) }
        require(values.isNotEmpty()) { "max requires at least one input" }
        return values.reduce(arithmetic.max)
    }
}

internal class MaximumNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<ContextPredicate>, arithmetic: NumberProviderArithmetic<T>) = StructCodec.struct(
        "inputs", providerCodec.list(), { (it as MaximumDeclaration<T>).inputs },
        { inputs -> MaximumDeclaration(inputs, arithmetic) },
    ).asProviderCodec()
}
