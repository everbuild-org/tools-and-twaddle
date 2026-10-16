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

private data class FloorDivideDeclaration<T>(val left: NumberProviderDeclaration<T>, val right: NumberProviderDeclaration<T>, val arithmetic: NumberProviderArithmetic<T>) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.FLOOR_DIV
    override fun get(context: NumberProviderContext, random: Random) = arithmetic.floorDivide(left.get(context, random), right.get(context, random))
}

internal class FloorDivideNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<NumberProviderCondition>, arithmetic: NumberProviderArithmetic<T>) = StructCodec.struct(
        "left", providerCodec, { (it as FloorDivideDeclaration<T>).left },
        "right", providerCodec, { it.right },
        { left, right -> FloorDivideDeclaration(left, right, arithmetic) },
    ).asProviderCodec()
}
