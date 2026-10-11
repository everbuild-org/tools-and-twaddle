package org.everbuild.twaddle.stacked_behaviour.engine.loop

import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleChainApplicator
import org.everbuild.twaddle.stacked_behaviour.RuleChainResult
import org.everbuild.twaddle.stacked_behaviour.RuleResult

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