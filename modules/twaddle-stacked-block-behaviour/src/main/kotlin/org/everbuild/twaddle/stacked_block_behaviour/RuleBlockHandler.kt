package org.everbuild.twaddle.stacked_block_behaviour

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.twaddle.stacked_block_behaviour.engine.RuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext

class RuleBlockHandler(
    private val key: Key,
    private val interaction: RuleChainApplicator<InteractionActivityContext>,
) : BlockHandler {
    override fun getKey(): Key = key

    override fun onInteract(interaction: BlockHandler.Interaction): Boolean {
        val result = this.interaction.apply(
            InteractionActivityContext(interaction)
        )

        return result.ruleResult !is RuleResult.Consume
    }
}

/**
 * Compiles definitions into handlers.
 *
 * Installation onto blocks and registration for persistence belong to the caller.
 */
fun BlockRuleset.createHandlers(
    factory: RuleChainApplicatorFactory,
    handlerKey: (blockKey: Key) -> Key,
): Map<Key, RuleBlockHandler> =
    blocks.mapValues { (blockKey, behaviour) ->
        RuleBlockHandler(
            key = handlerKey(blockKey),
            interaction = behaviour.interact
                ?.let { factory.create(it) }
                ?: RuleChainApplicator.empty(),
        )
    }