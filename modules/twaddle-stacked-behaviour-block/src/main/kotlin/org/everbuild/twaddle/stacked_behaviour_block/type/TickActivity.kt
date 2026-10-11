package org.everbuild.twaddle.stacked_behaviour_block.type

import net.minestom.server.coordinate.Point
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.ActivityKey
import org.everbuild.twaddle.stacked_behaviour.BehaviourEditor
import org.everbuild.twaddle.stacked_behaviour.RuleChainEditor
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

val TickActivity = ActivityKey<BlockRuleset, TickActivityContext>("twaddle:block_tick")

val BehaviourEditor<BlockRuleset>.tick: RuleChainEditor<TickActivityContext>
    @JvmSynthetic get() = activity(TickActivity)

data class TickActivityContext(
    val block: Block,
    val instance: Instance,
    val blockPosition: Point,
    val tagHandler: TagHandler = TagHandler.newHandler(),
) : ActivityContext {
    constructor(tick: BlockHandler.Tick) : this(tick.block, tick.instance, tick.blockPosition)

    fun replaceBlock(replacement: Block) {
        instance.setBlock(blockPosition, replacement)
    }

    override fun tagHandler(): TagHandler = tagHandler

    companion object
}
