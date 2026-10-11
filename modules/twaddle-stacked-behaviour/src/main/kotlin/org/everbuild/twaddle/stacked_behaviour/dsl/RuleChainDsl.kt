package org.everbuild.twaddle.stacked_behaviour.dsl

import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleChain

@StackedBehaviourDslMarker
class RuleChainDsl<T: ActivityContext> {
    internal val rules = mutableListOf<Rule<T>>()
    private val keys: HashSet<Rule.Key<T>> = hashSetOf()

    fun install(vararg rules: Rule<T>) {
        val incomingKeys = rules.map { it.key }

        require(incomingKeys.distinct().size == incomingKeys.size) {
            "Duplicate keys in installed rules"
        }

        require(incomingKeys.none { it in keys }) {
            "One or more rules are already installed"
        }

        this.rules.addAll(rules)
        keys.addAll(incomingKeys)
    }

    internal fun ruleChain(): RuleChain<T> = RuleChain(rules.toList())
}
