package org.everbuild.twaddle.stacked_block_behaviour.ruleset

import org.everbuild.twaddle.stacked_block_behaviour.RuleChain
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext

/**
 * Behavior contributions for one block type.
 *
 * A null chain means no contribution for that activity.
 * An empty chain is an explicit contribution that falls through.
 */
class BlockBehaviour(
    val interact: RuleChain<InteractionActivityContext>? = null,
)