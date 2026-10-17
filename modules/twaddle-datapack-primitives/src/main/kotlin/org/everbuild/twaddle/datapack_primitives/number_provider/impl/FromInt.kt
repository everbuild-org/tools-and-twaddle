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

private data class FromIntDeclaration(
    val input: Int,
    val arithmetic: NumberProviderArithmetic<Float>,
) : NumberProviderDeclaration<Float> {
    override fun type() = NumberProviderKeys.FROM_INT
    override fun get(context: DataContext, random: Random) = arithmetic.fromInt(input)
}

internal class FromIntNumberProviderType : NumberProviderType<Float> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<Float>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<ContextPredicate>,
        arithmetic: NumberProviderArithmetic<Float>,
    ) = StructCodec.struct(
        "input", Codec.INT,
        { (it as FromIntDeclaration).input },
        { input -> FromIntDeclaration(input, arithmetic) },
    ).asProviderCodec()
}
