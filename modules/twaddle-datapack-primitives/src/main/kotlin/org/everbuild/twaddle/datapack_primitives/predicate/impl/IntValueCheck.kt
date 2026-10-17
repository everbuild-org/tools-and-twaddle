package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class IntValueCheckPredicate(
    val value: NumberProviderDeclaration<Int>,
    val test: ProviderValueTest<Int>,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.INT_VALUE_CHECK

    override fun test(context: DataContext, random: Random): Boolean =
        test.matches(value.get(context, random), context, random) { value, min, max ->
            (min == null || value >= min) && (max == null || value <= max)
        }
}

internal class IntValueCheckPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<ContextPredicate> = StructCodec.struct(
        "value", intProviderCodec, IntValueCheckPredicate::value,
        "test", providerValueTestCodec(intProviderCodec), IntValueCheckPredicate::test,
        ::IntValueCheckPredicate,
    ).asContextPredicateCodec()
}
