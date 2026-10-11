package org.everbuild.twaddle.stacked_behaviour_block.type

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.ActivityKey
import org.everbuild.twaddle.stacked_behaviour.BehaviourEditor
import org.everbuild.twaddle.stacked_behaviour.RuleChainEditor
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

val PlacementActivity = ActivityKey<BlockRuleset, PlacementActivityContext>("twaddle:block_placement")

val BehaviourEditor<BlockRuleset>.place: RuleChainEditor<PlacementActivityContext>
    @JvmSynthetic get() = activity(PlacementActivity)

/** Player-specific fields are null for programmatic placements. */
data class PlacementActivityContext(
    val block: Block,
    val previousBlock: Block,
    val instance: Instance,
    val blockPosition: Point,
    val player: Player? = null,
    val hand: PlayerHand? = null,
    val blockFace: BlockFace? = null,
    val cursorX: Float? = null,
    val cursorY: Float? = null,
    val cursorZ: Float? = null,
    val tagHandler: TagHandler = TagHandler.newHandler(),
) : ActivityContext {
    constructor(placement: BlockHandler.Placement) : this(
        placement.block,
        placement.previousBlock,
        placement.instance,
        placement.blockPosition,
        (placement as? BlockHandler.PlayerPlacement)?.player,
        (placement as? BlockHandler.PlayerPlacement)?.hand,
        (placement as? BlockHandler.PlayerPlacement)?.blockFace,
        (placement as? BlockHandler.PlayerPlacement)?.cursorX,
        (placement as? BlockHandler.PlayerPlacement)?.cursorY,
        (placement as? BlockHandler.PlayerPlacement)?.cursorZ,
    )

    fun replaceBlock(replacement: Block) {
        instance.setBlock(blockPosition, replacement)
    }

    override fun tagHandler(): TagHandler = tagHandler

    companion object
}
