package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderKeys
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.ScoreContext
import org.everbuild.twaddle.datapack_primitives.context.ScoreTarget
import java.util.Random

private data class ScoreDeclaration<T>(
    val objective: String,
    val target: ScoreTarget,
    val fallback: Int?,
    val arithmetic: NumberProviderArithmetic<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.SCORE
    override fun get(context: DataContext, random: Random): T {
        val accessor = context.getBehaviour(ScoreContext)
        val score = accessor.getScore(target, objective) ?: fallback
            ?: error("No score '$objective' for target $target and no fallback was provided")
        return arithmetic.fromInt(score)
    }
}

internal class ScoreNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<ContextPredicate>,
        arithmetic: NumberProviderArithmetic<T>,
    ) = StructCodec.struct(
        "score", Codec.STRING, { (it as ScoreDeclaration<T>).objective },
        "target", StructCodec.struct(
            "type", Codec.STRING, ScoreTarget::type,
            "name", Codec.STRING.optional(), ScoreTarget::name,
            ::ScoreTarget,
        ), { it.target },
        "fallback", Codec.INT.optional(), { it.fallback },
        { objective, target, fallback -> ScoreDeclaration(objective, target, fallback, arithmetic) },
    ).asProviderCodec()
}
