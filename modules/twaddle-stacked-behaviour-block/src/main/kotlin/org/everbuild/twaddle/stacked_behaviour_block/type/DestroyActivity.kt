package org.everbuild.twaddle.stacked_behaviour_block.type

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.ActivityKey
import org.everbuild.twaddle.stacked_behaviour.BehaviourEditor
import org.everbuild.twaddle.stacked_behaviour.RuleChainEditor
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

val DestroyActivity = ActivityKey<BlockRuleset, DestroyActivityContext>("twaddle:block_destroy")

val BehaviourEditor<BlockRuleset>.destroy: RuleChainEditor<DestroyActivityContext>
    @JvmSynthetic get() = activity(DestroyActivity)

/** [player] is null for programmatic destruction or replacement. */
data class DestroyActivityContext(
    val block: Block,
    val newBlock: Block,
    val instance: Instance,
    val blockPosition: Point,
    val player: Player? = null,
    val tagHandler: TagHandler = TagHandler.newHandler(),
) : ActivityContext {
    constructor(destroy: BlockHandler.Destroy) : this(
        destroy.block,
        destroy.newBlock,
        destroy.instance,
        destroy.blockPosition,
        (destroy as? BlockHandler.PlayerDestroy)?.player,
    )

    fun replaceBlock(replacement: Block) {
        instance.setBlock(blockPosition, replacement)
    }

    override fun tagHandler(): TagHandler = tagHandler

    companion object
}
