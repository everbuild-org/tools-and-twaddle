package org.everbuild.twaddle.stacked_behaviour_block.type

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Entity
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.ActivityKey
import org.everbuild.twaddle.stacked_behaviour.BehaviourEditor
import org.everbuild.twaddle.stacked_behaviour.RuleChainEditor
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

val TouchActivity = ActivityKey<BlockRuleset, TouchActivityContext>("twaddle:block_touch")

val BehaviourEditor<BlockRuleset>.touch: RuleChainEditor<TouchActivityContext>
    @JvmSynthetic get() = activity(TouchActivity)

data class TouchActivityContext(
    val block: Block,
    val instance: Instance,
    val blockPosition: Point,
    val touching: Entity,
    val tagHandler: TagHandler = TagHandler.newHandler(),
) : ActivityContext {
    constructor(touch: BlockHandler.Touch) : this(touch.block, touch.instance, touch.blockPosition, touch.touching)

    fun replaceBlock(replacement: Block) {
        instance.setBlock(blockPosition, replacement)
    }

    override fun tagHandler(): TagHandler = tagHandler

    companion object
}
