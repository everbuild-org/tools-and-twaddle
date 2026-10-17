package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class FloatValueCheckPredicate(
    val value: NumberProviderDeclaration<Float>,
    val test: ProviderValueTest<Float>,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.FLOAT_VALUE_CHECK

    override fun test(context: DataContext, random: Random): Boolean =
        test.matches(value.get(context, random), context, random) { value, min, max ->
            (min == null || value >= min) && (max == null || value <= max)
        }
}

internal class FloatValueCheckPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<ContextPredicate> = StructCodec.struct(
        "value", floatProviderCodec, FloatValueCheckPredicate::value,
        "test", providerValueTestCodec(floatProviderCodec), FloatValueCheckPredicate::test,
        ::FloatValueCheckPredicate,
    ).asContextPredicateCodec()
}
