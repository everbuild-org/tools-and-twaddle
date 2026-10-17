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

private data class BinomialDeclaration<T>(
    val n: NumberProviderDeclaration<T>,
    val p: NumberProviderDeclaration<Float>,
    val arithmetic: NumberProviderArithmetic<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.BINOMIAL
    override fun get(context: DataContext, random: Random): T =
        arithmetic.binomial?.invoke(n.get(context, random), p.get(context, random), random)
            ?: error("Binomial providers are not available for this value type")
}

internal class BinomialNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<ContextPredicate>,
        arithmetic: NumberProviderArithmetic<T>,
    ) = StructCodec.struct(
        "n", providerCodec, { (it as BinomialDeclaration<T>).n },
        "p", requireNotNull(floatProviderCodec), { it.p },
        { n, p -> BinomialDeclaration(n, p, arithmetic) },
    ).asProviderCodec()
}
