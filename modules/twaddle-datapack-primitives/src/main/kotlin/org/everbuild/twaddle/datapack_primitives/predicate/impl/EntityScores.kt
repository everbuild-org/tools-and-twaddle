package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.ScoreContext
import org.everbuild.twaddle.datapack_primitives.context.ScoreTarget
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class EntityScoresPredicate(
    val entity: String,
    val scores: Map<String, ProviderValueTest<Int>>,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.ENTITY_SCORES

    override fun test(context: DataContext, random: Random): Boolean {
        val accessor = context[ScoreContext]
        val target = ScoreTarget(entity, null)
        return scores.all { (objective, test) ->
            val value = accessor.getScore(target, objective) ?: return@all false
            test.matches(value, context, random) { actual, min, max ->
                (min == null || actual >= min) && (max == null || actual <= max)
            }
        }
    }
}

internal class EntityScoresPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "entity", Codec.STRING, EntityScoresPredicate::entity,
        "scores", Codec.STRING.mapValue(providerValueTestCodec(intProviderCodec)), EntityScoresPredicate::scores,
        ::EntityScoresPredicate,
    ).asContextPredicateCodec()
}
