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

val InteractionActivity = ActivityKey<BlockRuleset, InteractionActivityContext>("twaddle:block_interaction")

val BehaviourEditor<BlockRuleset>.interact: RuleChainEditor<InteractionActivityContext>
    @JvmSynthetic get() = activity(InteractionActivity)

data class InteractionActivityContext(
    val block: Block,
    val instance: Instance,
    val blockFace: BlockFace,
    val blockPosition: Point,
    val cursorPosition: Point,
    val player: Player,
    val hand: PlayerHand,
    val tagHandler: TagHandler = TagHandler.newHandler(),
) : ActivityContext {
    constructor(interaction: BlockHandler.Interaction) : this(
        interaction.block,
        interaction.instance,
        interaction.blockFace,
        interaction.blockPosition,
        interaction.cursorPosition,
        interaction.player,
        interaction.hand
    )

    fun replaceBlock(replacement: Block) {
        instance.setBlock(blockPosition, replacement)
    }

    override fun tagHandler(): TagHandler = tagHandler

    companion object
}
