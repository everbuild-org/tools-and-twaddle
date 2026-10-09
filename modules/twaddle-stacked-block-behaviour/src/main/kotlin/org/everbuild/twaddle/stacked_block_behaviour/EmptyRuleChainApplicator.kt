package org.everbuild.twaddle.stacked_block_behaviour

object EmptyRuleChainApplicator : RuleChainApplicator<ActivityContext> {
    override fun apply(context: ActivityContext): RuleChainResult<ActivityContext> {
        return RuleChainResult(
            RuleResult.next(),
            context
        )
    }
}