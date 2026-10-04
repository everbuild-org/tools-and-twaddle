package org.everbuild.twaddle.stacked_block_behaviour

data class RuleChainResult<T : ActivityContext>(
    val ruleResult: RuleResult<T>,
    val finalContext: T,
)