package org.everbuild.twaddle.stacked_behaviour_block

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.stacked_behaviour.Behaviour
import org.everbuild.twaddle.stacked_behaviour.RuleChainApplicator
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.engine.RuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.configuration.BlockEntityConfiguration
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext

open class RuleBlockHandler @JvmOverloads constructor(
    private val key: Key,
    private val interaction: RuleChainApplicator<InteractionActivityContext>,
    private val blockEntity: BlockEntityConfiguration = BlockEntityConfiguration(),
) : BlockHandler {
    override fun getKey(): Key = key

    override fun getBlockEntityAction(): Byte = blockEntity.action
    override fun getBlockEntityTags(): Collection<Tag<*>> = blockEntity.tags

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val result = this.interaction.apply(
            InteractionActivityContext(interaction)
        )

        return result.ruleResult !is RuleResult.Consume
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
    ): RuleBlockHandler = RuleBlockHandler(
        handlerKey,
        interaction = definition.createApplicator(InteractionActivity, ruleChainFactory),
        blockEntity = definition.configuration(BlockEntityConfiguration::class.java) ?: BlockEntityConfiguration(),
    )
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
