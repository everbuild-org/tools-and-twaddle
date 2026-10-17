package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class InvertedPredicate(val term: ContextPredicate) : ContextPredicate {
    override fun type() = ContextPredicateKeys.INVERTED

    override fun test(context: DataContext, random: Random) =
        !term.test(context, random)
}

internal class InvertedPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<ContextPredicate> = StructCodec.struct(
        "term", predicateCodec, InvertedPredicate::term,
        ::InvertedPredicate,
    ).asContextPredicateCodec()
}
