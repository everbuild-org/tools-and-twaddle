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

private data class FromFloatDeclaration<T>(val input: NumberProviderDeclaration<Float>, val arithmetic: NumberProviderArithmetic<T>) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.FROM_FLOAT
    override fun get(context: DataContext, random: Random) = arithmetic.fromFloat(input.get(context, random))
}

internal class FromFloatNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(providerCodec: Codec<NumberProviderDeclaration<T>>, floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?, conditionCodec: Codec<ContextPredicate>, arithmetic: NumberProviderArithmetic<T>): StructCodec<NumberProviderDeclaration<T>> {
        val floatCodec = requireNotNull(floatProviderCodec) { "from_float requires a float number-provider codec" }
        return StructCodec.struct(
            "input", floatCodec, { (it as FromFloatDeclaration<T>).input },
            { input -> FromFloatDeclaration(input, arithmetic) },
        ).asProviderCodec()
    }
}
