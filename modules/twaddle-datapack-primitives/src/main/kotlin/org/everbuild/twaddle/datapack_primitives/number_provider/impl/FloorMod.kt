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

private data class FloorModDeclaration<T>(val left: NumberProviderDeclaration<T>, val right: NumberProviderDeclaration<T>, val arithmetic: NumberProviderArithmetic<T>) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.FLOOR_MOD
    override fun get(context: DataContext, random: Random) = arithmetic.floorMod(left.get(context, random), right.get(context, random))
}

internal class FloorModNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<ContextPredicate>, arithmetic: NumberProviderArithmetic<T>) = StructCodec.struct(
        "left", providerCodec, { (it as FloorModDeclaration<T>).left },
        "right", providerCodec, { it.right },
        { left, right -> FloorModDeclaration(left, right, arithmetic) },
    ).asProviderCodec()
}
