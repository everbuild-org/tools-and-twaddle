package org.everbuild.twaddle.stacked_block_behaviour

import org.everbuild.twaddle.stacked_block_behaviour.dsl.RuleChainDsl
import java.util.function.Consumer

class RuleChain<T : ActivityContext>(ruleList: List<Rule<T>>) {
    private val ruleList = ruleList.toList()

    init {
        require(ruleList.map { it.key }.toSet().size == ruleList.size) { "Rule keys must be unique" }
    }

    internal fun rules(): List<Rule<T>> = ruleList

    companion object {
        @JvmName("__kt_build")
        fun <T : ActivityContext> build(block: RuleChainDsl<T>.() -> Unit): RuleChain<T> =
            RuleChainDsl<T>().apply(block).ruleChain()

        @JvmName("build")
        @JvmStatic
        fun <T : ActivityContext> jvmBuild(block: Consumer<RuleChainDsl<T>>): RuleChain<T> =
            build { block.accept(this) }
    }
}