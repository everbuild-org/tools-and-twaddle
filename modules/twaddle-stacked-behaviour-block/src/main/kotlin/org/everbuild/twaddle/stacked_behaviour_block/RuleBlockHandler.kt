package org.everbuild.twaddle.stacked_behaviour_block

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.common.block.tick.RandomTickHandler
import org.everbuild.twaddle.stacked_behaviour.Behaviour
import org.everbuild.twaddle.stacked_behaviour.RuleChainApplicator
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.engine.RuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.configuration.BlockEntityConfiguration
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TouchActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.TouchActivityContext

open class RuleBlockHandler @JvmOverloads constructor(
    private val key: Key,
    private val interaction: RuleChainApplicator<InteractionActivityContext>,
    private val blockEntity: BlockEntityConfiguration = BlockEntityConfiguration(),
    private val ticking: RuleChainApplicator<TickActivityContext>? = null,
    private val placement: RuleChainApplicator<PlacementActivityContext> = RuleChainApplicator.empty(),
    private val destroy: RuleChainApplicator<DestroyActivityContext> = RuleChainApplicator.empty(),
    private val touch: RuleChainApplicator<TouchActivityContext> = RuleChainApplicator.empty(),
) : BlockHandler {
    override fun getKey(): Key = key

    override fun getBlockEntityAction(): Byte = blockEntity.action
    override fun getBlockEntityTags(): Collection<Tag<*>> = blockEntity.tags

    override fun isTickable(): Boolean = ticking != null

    override fun tick(tick: BlockHandler.Tick) {
        ticking?.apply(TickActivityContext(tick))
    }

    override fun onPlace(placement: BlockHandler.Placement) {
        this.placement.apply(PlacementActivityContext(placement))
    }

    override fun onDestroy(destroy: BlockHandler.Destroy) {
        this.destroy.apply(DestroyActivityContext(destroy))
    }

    override fun onTouch(touch: BlockHandler.Touch) {
        this.touch.apply(TouchActivityContext(touch))
    }

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val result = this.interaction.apply(
            InteractionActivityContext(interaction)
        )

        return result.ruleResult !is RuleResult.Consume
    }
}

open class RandomTickRuleBlockHandler @JvmOverloads constructor(
    key: Key,
    interaction: RuleChainApplicator<InteractionActivityContext>,
    blockEntity: BlockEntityConfiguration = BlockEntityConfiguration(),
    ticking: RuleChainApplicator<TickActivityContext>? = null,
    placement: RuleChainApplicator<PlacementActivityContext> = RuleChainApplicator.empty(),
    destroy: RuleChainApplicator<DestroyActivityContext> = RuleChainApplicator.empty(),
    touch: RuleChainApplicator<TouchActivityContext> = RuleChainApplicator.empty(),
    private val randomTick: RuleChainApplicator<RandomTickActivityContext> = RuleChainApplicator.empty(),
) : RuleBlockHandler(
    key,
    interaction,
    blockEntity,
    ticking,
    placement,
    destroy,
    touch,
), RandomTickHandler {
    override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block? {
        val result = this.randomTick.apply(RandomTickActivityContext(randomTick))
        return result.finalContext.newBlock
    }
}

fun interface BlockHandlerFactory<Target> {
    fun create(
        handlerKey: Key,
        definition: Behaviour<BlockRuleset>,
        ruleChainFactory: RuleChainApplicatorFactory,
    ): Target
}

object RuleBlockHandlerFactory : BlockHandlerFactory<BlockHandler> {
    override fun create(
        handlerKey: Key,
        definition: Behaviour<BlockRuleset>,
        ruleChainFactory: RuleChainApplicatorFactory,
    ): RuleBlockHandler {
        val interaction = definition.createApplicator(InteractionActivity, ruleChainFactory)
        val blockEntity =
            definition.configuration(BlockEntityConfiguration::class.java) ?: BlockEntityConfiguration.EMPTY
        val ticking = definition.activity(TickActivity)?.let { ruleChainFactory.create(it) }
        val placement = definition.createApplicator(PlacementActivity, ruleChainFactory)
        val destroy = definition.createApplicator(DestroyActivity, ruleChainFactory)
        val touch = definition.createApplicator(TouchActivity, ruleChainFactory)
        val randomTick = definition.activity(RandomTickActivity)?.let { ruleChainFactory.create(it) }
        return if (randomTick == null) {
            RuleBlockHandler(handlerKey, interaction, blockEntity, ticking, placement, destroy, touch)
        } else {
            RandomTickRuleBlockHandler(
                handlerKey,
                interaction,
                blockEntity,
                ticking,
                placement,
                destroy,
                touch,
                randomTick
            )
        }
    }
}

/**
 * Compiles definitions into handlers.
 *
 * Installation onto blocks and registration for persistence belong to the caller.
 */
fun <T> BlockRuleset.createHandlers(
    ruleChainFactory: RuleChainApplicatorFactory,
    blockHandlerFactory: BlockHandlerFactory<T>,
    handlerKey: (blockKey: Key) -> Key,
): Map<Key, T> =
    blocks.mapValues { (blockKey, behaviour) ->
        blockHandlerFactory.create(handlerKey(blockKey), behaviour, ruleChainFactory)
    }
