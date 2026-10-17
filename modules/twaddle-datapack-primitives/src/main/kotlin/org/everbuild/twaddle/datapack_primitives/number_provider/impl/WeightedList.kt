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

private data class WeightedProvider<T>(val data: NumberProviderDeclaration<T>, val weight: Int)

private data class WeightedListDeclaration<T>(
    val distribution: List<WeightedProvider<T>>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.WEIGHTED_LIST

    override fun get(context: DataContext, random: Random): T {
        val totalWeight = distribution.sumOf { it.weight.toLong() }
        require(totalWeight > 0 && distribution.all { it.weight > 0 }) { "weighted_list requires positive weights" }
        var selectedWeight = random.nextLong(totalWeight)
        val selected = distribution.first { entry ->
            selectedWeight -= entry.weight
            selectedWeight < 0
        }
        return selected.data.get(context, random)
    }
}

internal class WeightedListNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<ContextPredicate>,
        arithmetic: NumberProviderArithmetic<T>,
    ): StructCodec<NumberProviderDeclaration<T>> {
        val entryCodec = StructCodec.struct(
            "data", providerCodec, WeightedProvider<T>::data,
            "weight", Codec.INT, WeightedProvider<T>::weight,
            ::WeightedProvider,
        )
        return StructCodec.struct(
            "distribution", entryCodec.list(), { (it as WeightedListDeclaration<T>).distribution },
            { distribution -> WeightedListDeclaration(distribution) },
        ).asProviderCodec()
    }
}
