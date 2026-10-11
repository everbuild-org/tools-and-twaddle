package org.everbuild.twaddle.stacked_behaviour_block.ruleset

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.createHandlers

open class BlockRulesetApplicator {
    @JvmOverloads
    fun applyRulesAsDefault(ruleset: BlockRuleset, namespace: String = "minecraft") {
        ruleset.createHandlers(
            LoopingRuleChainApplicatorFactory(),
            handlerKey = { blockKey ->
                Key.key(namespace, blockKey.value())
            },
        ).forEach { (key, handler) ->
            MinecraftServer.getBlockManager().registerHandler(key) { handler }
        }
    }
}