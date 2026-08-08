package org.everbuild.twaddle.stacked_block_behaviour.engine

import org.everbuild.twaddle.stacked_block_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.RuleChain
import org.everbuild.twaddle.stacked_block_behaviour.RuleChainApplicator

interface RuleChainApplicatorFactory {
    fun <T : ActivityContext> create(ruleset: RuleChain<T>): RuleChainApplicator<T>
}