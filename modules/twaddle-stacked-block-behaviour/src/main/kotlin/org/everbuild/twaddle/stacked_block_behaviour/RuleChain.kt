package org.everbuild.twaddle.stacked_block_behaviour

import org.everbuild.twaddle.stacked_block_behaviour.dsl.RuleChainDsl

class RuleChain<T : ActivityContext>(private val ruleList: List<Rule<T>>) {
    internal fun rules(): List<Rule<T>> = ruleList

    companion object {
        fun <T : ActivityContext> build(block: RuleChainDsl<T>.() -> Unit): RuleChain<T> =
            RuleChainDsl<T>().apply(block).ruleChain()
    }
}