package org.everbuild.twaddle.stacked_behaviour_block.ruleset

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.twaddle.stacked_behaviour.engine.RuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.BlockHandlerFactory
import org.everbuild.twaddle.stacked_behaviour_block.RuleBlockHandlerFactory
import org.everbuild.twaddle.stacked_behaviour_block.createHandlers

open class BlockRulesetApplicator {
    @JvmOverloads
    fun applyRulesAsDefault(
        ruleset: BlockRuleset,
        namespace: String = "minecraft",
        ruleChainApplicatorFactory: RuleChainApplicatorFactory = LoopingRuleChainApplicatorFactory(),
        blockHandlerFactory: BlockHandlerFactory<BlockHandler> = RuleBlockHandlerFactory
    ) {
        ruleset.createHandlers(
            ruleChainFactory = ruleChainApplicatorFactory,
            handlerKey = { blockKey ->
                Key.key(namespace, "${blockKey.namespace()}/${blockKey.value()}")
            },
            blockHandlerFactory = blockHandlerFactory
        ).forEach { (key, handler) ->
            MinecraftServer.getBlockManager().registerHandler(key) { handler }
        }
    }
}