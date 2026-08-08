package org.everbuild.twaddle.stacked_block_behaviour

fun interface RuleChainApplicator<T : ActivityContext> {
    fun apply(context: T): RuleResult<T>
}

