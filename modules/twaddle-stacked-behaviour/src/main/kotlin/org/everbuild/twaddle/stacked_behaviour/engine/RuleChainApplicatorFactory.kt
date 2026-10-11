package org.everbuild.twaddle.stacked_behaviour.engine

import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.RuleChain
import org.everbuild.twaddle.stacked_behaviour.RuleChainApplicator

interface RuleChainApplicatorFactory {
    fun <T : ActivityContext> create(ruleset: RuleChain<T>): RuleChainApplicator<T>
}