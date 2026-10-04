package org.everbuild.twaddle.stacked_block_behaviour.dsl

import org.everbuild.twaddle.stacked_block_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.Rule
import org.everbuild.twaddle.stacked_block_behaviour.RuleChain

@StackedBehaviourDslMarker
class RuleChainDsl<T: ActivityContext> {
    internal val rules = mutableListOf<Rule<T>>()
    private val keys: HashSet<Rule.Key<T>> = hashSetOf()

    fun install(rule: Rule<T>) {
        require(keys.add(rule.key)) { "Rule with key ${rule.key} already installed" }
        rules += rule
    }

    internal fun ruleChain(): RuleChain<T> = RuleChain(rules.toList())
}