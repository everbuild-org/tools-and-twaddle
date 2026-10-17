package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class RandomChancePredicate(
    val chance: NumberProviderDeclaration<Float>,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.RANDOM_CHANCE

    override fun test(context: DataContext, random: Random) =
        random.nextFloat() < chance.get(context, random)
}

internal class RandomChancePredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<ContextPredicate> = StructCodec.struct(
        "chance", floatProviderCodec, RandomChancePredicate::chance,
        ::RandomChancePredicate,
    ).asContextPredicateCodec()
}
