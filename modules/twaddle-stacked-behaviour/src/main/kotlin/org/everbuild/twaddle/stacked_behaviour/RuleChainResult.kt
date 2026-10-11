package org.everbuild.twaddle.stacked_behaviour

data class RuleChainResult<T : ActivityContext>(
    val ruleResult: RuleResult<T>,
    val finalContext: T,
)