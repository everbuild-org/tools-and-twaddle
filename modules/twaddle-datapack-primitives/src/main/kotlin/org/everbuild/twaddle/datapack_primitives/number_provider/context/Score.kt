package org.everbuild.twaddle.datapack_primitives.number_provider.context

interface ScoreAccessor {
    fun getScore(objective: String): Int

    fun getScore(target: ScoreTarget, objective: String): Int? = getScore(objective)
}

data class ScoreTarget(val type: String, val name: String?)

val ScoreContext = NumberProviderContextKey<ScoreAccessor>("twaddle:score_context")

@JvmSynthetic
fun NumberProviderContext.withScores(accessor: ScoreAccessor) = withBehaviour(ScoreContext, accessor)
