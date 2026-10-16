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

private data class SubtractDeclaration<T>(val left: NumberProviderDeclaration<T>, val right: NumberProviderDeclaration<T>, val arithmetic: NumberProviderArithmetic<T>) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.SUB
    override fun get(context: NumberProviderContext, random: Random) = arithmetic.add(left.get(context, random), arithmetic.negate(right.get(context, random)))
}

internal class SubtractNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<NumberProviderCondition>, arithmetic: NumberProviderArithmetic<T>) = StructCodec.struct(
        "left", providerCodec, { (it as SubtractDeclaration<T>).left },
        "right", providerCodec, { it.right },
        { left, right -> SubtractDeclaration(left, right, arithmetic) },
    ).asProviderCodec()
}
