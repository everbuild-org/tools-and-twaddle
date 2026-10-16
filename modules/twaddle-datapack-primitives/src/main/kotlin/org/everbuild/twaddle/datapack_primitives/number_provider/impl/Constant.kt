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

internal data class ConstantNumberProviderDeclaration<T>(val value: T) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.CONSTANT
    override fun get(context: NumberProviderContext, random: Random) = value
}

internal class ConstantNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<NumberProviderCondition>,
        arithmetic: NumberProviderArithmetic<T>,
    ) = StructCodec.struct(
        "value",
        arithmetic.valueCodec,
        { (it as ConstantNumberProviderDeclaration<T>).value },
        ::ConstantNumberProviderDeclaration,
    ).asProviderCodec()
}
