package org.everbuild.twaddle.stacked_behaviour_block

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.twaddle.stacked_behaviour.RuleChainApplicator
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.engine.RuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext

open class RuleBlockHandler(
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
interface BlockHandlerFactory {
    fun create(blockKey: Key): BlockHandler
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
            interaction = behaviour.createApplicator(InteractionActivity, factory),
        )
    }