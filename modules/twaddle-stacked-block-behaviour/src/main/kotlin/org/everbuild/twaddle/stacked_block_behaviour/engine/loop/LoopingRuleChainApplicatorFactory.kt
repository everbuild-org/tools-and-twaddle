package org.everbuild.twaddle.stacked_block_behaviour.engine.loop

import org.everbuild.twaddle.stacked_block_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.RuleChain
import org.everbuild.twaddle.stacked_block_behaviour.RuleChainApplicator
import org.everbuild.twaddle.stacked_block_behaviour.engine.RuleChainApplicatorFactory

class LoopingRuleChainApplicatorFactory : RuleChainApplicatorFactory {
    override fun <T : ActivityContext> create(ruleset: RuleChain<T>): RuleChainApplicator<T> =
        LoopingRuleChainApplicatorImpl(ruleset.rules())
}