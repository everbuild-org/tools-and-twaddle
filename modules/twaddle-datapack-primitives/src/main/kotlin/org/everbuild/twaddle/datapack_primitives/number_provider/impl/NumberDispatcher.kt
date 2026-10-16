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

private data class NumberDispatcherCase<T>(val condition: NumberProviderCondition, val value: NumberProviderDeclaration<T>)

private data class NumberDispatcherDeclaration<T>(
    val cases: List<NumberDispatcherCase<T>>,
    val default: NumberProviderDeclaration<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.NUMBER_DISPATCHER
    override fun get(context: NumberProviderContext, random: Random) =
        cases.firstOrNull { it.condition.test(context, random) }?.value?.get(context, random) ?: default.get(context, random)
}

internal class NumberDispatcherNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<NumberProviderCondition>,
        arithmetic: NumberProviderArithmetic<T>,
    ): StructCodec<NumberProviderDeclaration<T>> {
        val caseCodec = StructCodec.struct(
            "condition", conditionCodec, NumberDispatcherCase<T>::condition,
            "value", providerCodec, NumberDispatcherCase<T>::value,
            ::NumberDispatcherCase,
        )
        return StructCodec.struct(
            "cases", caseCodec.list(), { (it as NumberDispatcherDeclaration<T>).cases },
            "default", providerCodec, { it.default },
            { cases, default -> NumberDispatcherDeclaration(cases, default) },
        ).asProviderCodec()
    }
}
