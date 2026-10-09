package org.everbuild.twaddle.stacked_block_behaviour

fun interface RuleChainApplicator<T : ActivityContext> {
    fun apply(context: T): RuleChainResult<T>

    companion object {
        @Suppress("UNCHECKED_CAST")
        fun <T : ActivityContext> empty(): RuleChainApplicator<T> =
            EmptyRuleChainApplicator as RuleChainApplicator<T>
    }
}

