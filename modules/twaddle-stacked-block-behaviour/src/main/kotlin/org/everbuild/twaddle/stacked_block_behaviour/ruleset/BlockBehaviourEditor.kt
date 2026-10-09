package org.everbuild.twaddle.stacked_block_behaviour.ruleset

import org.everbuild.twaddle.stacked_block_behaviour.RuleChainEditor
import org.everbuild.twaddle.stacked_block_behaviour.dsl.StackedBehaviourDslMarker

@StackedBehaviourDslMarker
class BlockBehaviourEditor internal constructor(
    behaviour: BlockBehaviour,
) {
    val interact = RuleChainEditor(behaviour.interact)

    internal fun build(): BlockBehaviour =
        BlockBehaviour(interact = interact.build())
}