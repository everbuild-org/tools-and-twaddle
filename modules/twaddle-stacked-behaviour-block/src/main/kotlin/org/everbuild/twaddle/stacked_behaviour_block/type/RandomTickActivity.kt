package org.everbuild.twaddle.stacked_behaviour_block.type

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.common.block.tick.RandomTickHandler
import org.everbuild.twaddle.stacked_behaviour.ActivityContext
import org.everbuild.twaddle.stacked_behaviour.ActivityKey
import org.everbuild.twaddle.stacked_behaviour.BehaviourEditor
import org.everbuild.twaddle.stacked_behaviour.RuleChainEditor
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

val RandomTickActivity = ActivityKey<BlockRuleset, RandomTickActivityContext>("twaddle:block_random_tick")

val BehaviourEditor<BlockRuleset>.randomTick: RuleChainEditor<RandomTickActivityContext>
    @JvmSynthetic get() = activity(RandomTickActivity)

data class RandomTickActivityContext(
    val block: Block,
    val instance: Instance,
    val blockPosition: BlockVec,
    val tagHandler: TagHandler = TagHandler.newHandler(),
    var newBlock: Block? = null,
) : ActivityContext {
    constructor(randomTick: RandomTickHandler.RandomTick) : this(
        randomTick.block,
        randomTick.instance,
        randomTick.blockPosition,
    )

    fun replaceBlock(replacement: Block) {
        newBlock = replacement
    }

    override fun tagHandler(): TagHandler = tagHandler

    companion object
}
