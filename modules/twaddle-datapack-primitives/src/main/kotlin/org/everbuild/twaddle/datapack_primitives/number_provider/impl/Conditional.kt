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

private data class ConditionalDeclaration<T>(
    val condition: NumberProviderCondition,
    val onTrue: NumberProviderDeclaration<T>,
    val onFalse: NumberProviderDeclaration<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.CONDITIONAL
    override fun get(context: NumberProviderContext, random: Random) =
        if (condition.test(context, random)) onTrue.get(context, random) else onFalse.get(context, random)
}

internal class ConditionalNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<NumberProviderCondition>,
        arithmetic: NumberProviderArithmetic<T>,
    ) = StructCodec.struct(
        "condition", conditionCodec, { (it as ConditionalDeclaration<T>).condition },
        "on_true", providerCodec, { it.onTrue },
        "on_false", providerCodec, { it.onFalse },
        { condition, onTrue, onFalse -> ConditionalDeclaration(condition, onTrue, onFalse) },
    ).asProviderCodec()
}
