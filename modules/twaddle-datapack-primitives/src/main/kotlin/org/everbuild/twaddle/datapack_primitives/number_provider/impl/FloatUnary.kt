package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

internal abstract class FloatUnaryNumberProviderType(
    private val key: net.kyori.adventure.key.Key,
    private val operation: (Float) -> Float,
) : NumberProviderType<Float> {
    private data class Declaration(
        val key: net.kyori.adventure.key.Key,
        val input: NumberProviderDeclaration<Float>,
        val operation: (Float) -> Float,
    ) : NumberProviderDeclaration<Float> {
        override fun type() = key
        override fun get(context: DataContext, random: Random) = operation(input.get(context, random))
    }

    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<Float>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<ContextPredicate>,
        arithmetic: NumberProviderArithmetic<Float>,
    ) = StructCodec.struct(
        "input", providerCodec,
        { (it as Declaration).input },
        { input -> Declaration(key, input, operation) },
    ).asProviderCodec()
}
