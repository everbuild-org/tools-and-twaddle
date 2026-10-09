package org.everbuild.twaddle.stacked_block_behaviour.ruleset

import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import org.everbuild.twaddle.stacked_block_behaviour.createHandlers
import org.everbuild.twaddle.stacked_block_behaviour.engine.loop.LoopingRuleChainApplicatorFactory

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