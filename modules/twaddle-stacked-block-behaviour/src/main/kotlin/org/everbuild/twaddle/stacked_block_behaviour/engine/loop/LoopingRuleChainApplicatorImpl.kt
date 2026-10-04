package org.everbuild.twaddle.stacked_block_behaviour.engine.loop

import org.everbuild.twaddle.stacked_block_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.Rule
import org.everbuild.twaddle.stacked_block_behaviour.RuleResult
import org.everbuild.twaddle.stacked_block_behaviour.RuleChainApplicator
import org.everbuild.twaddle.stacked_block_behaviour.RuleChainResult

internal class LoopingRuleChainApplicatorImpl<T : ActivityContext>(private val rules: List<Rule<T>>) :
    RuleChainApplicator<T> {
    override fun apply(context: T): RuleChainResult<T> {
        var currentContext = context
        var lastResult = RuleResult.next<T>()
        for (rule in rules) {
            lastResult = rule.evaluate(currentContext)
            when (lastResult) {
                is RuleResult.Cascade<T> -> currentContext = lastResult.newContext
                RuleResult.Consume -> return RuleChainResult(lastResult, currentContext)
                RuleResult.Next -> {}
            }
        }
        return  RuleChainResult(lastResult, currentContext)
    }
}