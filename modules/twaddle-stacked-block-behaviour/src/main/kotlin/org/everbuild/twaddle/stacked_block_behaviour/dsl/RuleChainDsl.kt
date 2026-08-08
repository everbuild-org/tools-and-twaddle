package org.everbuild.twaddle.stacked_block_behaviour.dsl

import org.everbuild.twaddle.stacked_block_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.Rule
import org.everbuild.twaddle.stacked_block_behaviour.RuleChain

@StackedBehaviourDslMarker
class RuleChainDsl<T: ActivityContext> {
    internal val rules = mutableListOf<Rule<T>>()

    internal fun ruleChain(): RuleChain<T> = RuleChain(rules.toList())
}