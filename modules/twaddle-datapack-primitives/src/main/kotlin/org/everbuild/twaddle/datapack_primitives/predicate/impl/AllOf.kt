package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class AllOfPredicate(val terms: List<ContextPredicate>) : ContextPredicate {
    override fun type() = ContextPredicateKeys.ALL_OF

    override fun test(context: DataContext, random: Random) =
        terms.all { it.test(context, random) }
}

internal class AllOfPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<ContextPredicate> = StructCodec.struct(
        "terms", predicateCodec.list(), AllOfPredicate::terms,
        ::AllOfPredicate,
    ).asContextPredicateCodec()
}
