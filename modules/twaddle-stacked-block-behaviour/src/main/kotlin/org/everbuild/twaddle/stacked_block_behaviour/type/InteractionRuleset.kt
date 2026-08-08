package org.everbuild.twaddle.stacked_block_behaviour.type

import net.minestom.server.coordinate.Point
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_block_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.RuleChain
import org.everbuild.twaddle.stacked_block_behaviour.RuleChainApplicator
import org.everbuild.twaddle.stacked_block_behaviour.engine.RuleChainApplicatorFactory


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

    override fun tagHandler(): TagHandler = tagHandler

    companion object
}

fun RuleChain<InteractionActivityContext>.createApplicator(factory: RuleChainApplicatorFactory): RuleChainApplicator<InteractionActivityContext> =
    factory.create(this)