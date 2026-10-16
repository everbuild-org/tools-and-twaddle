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

private data class NegateDeclaration<T>(val input: NumberProviderDeclaration<T>, val arithmetic: NumberProviderArithmetic<T>) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.NEGATE
    override fun get(context: NumberProviderContext, random: Random) = arithmetic.negate(input.get(context, random))
}

internal class NegateNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<NumberProviderCondition>, arithmetic: NumberProviderArithmetic<T>) = StructCodec.struct(
        "input", providerCodec, { (it as NegateDeclaration<T>).input },
        { input -> NegateDeclaration(input, arithmetic) },
    ).asProviderCodec()
}
