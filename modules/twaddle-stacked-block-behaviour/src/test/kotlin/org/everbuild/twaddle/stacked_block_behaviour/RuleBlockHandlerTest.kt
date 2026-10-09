package org.everbuild.twaddle.stacked_block_behaviour

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.stacked_block_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_block_behaviour.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext

val RuleBlockHandlerTests by testSuite {
    val outcomes: List<Pair<String, (InteractionActivityContext) -> RuleResult<InteractionActivityContext>>> = listOf(
        "Next" to { RuleResult.Next },
        "Cascade" to { RuleResult.Cascade(it.copy(block = Block.DIAMOND_BLOCK)) },
        "Consume" to { RuleResult.Consume },
    )
    outcomes.forEach { (name, outcome) ->
        envTest("$name ${if (name == "Consume") "blocks" else "allows"} further interaction processing") { env ->
            val instance = env.createEmptyInstance()
            val player = env.createPlayer(instance, Pos.ZERO)
            val blockPosition = Vec(1.0, 2.0, 3.0)
            val cursorPosition = Vec(0.25, 0.5, 0.75)
            val seen = mutableListOf<InteractionActivityContext>()
            val ruleset = BlockRuleset.build {
                addBlock(Block.STONE) {
                    interact.replace {
                        install(Rule(Rule.Key("test:outcome")) {
                            seen += it
                            outcome(it)
                        })
                    }
                }
            }
            val handlerKey = Key.key("test:stone_handler")
            val handler = ruleset.createHandlers(LoopingRuleChainApplicatorFactory()) { handlerKey }
                .getValue(Block.STONE.key())
            val interaction = BlockHandler.Interaction(
                Block.STONE, instance, BlockFace.NORTH, blockPosition, cursorPosition, player, PlayerHand.OFF,
            )

            handler.onInteract(interaction) shouldBe (name != "Consume")

            handler.key shouldBe handlerKey
            seen.size shouldBe 1
            val context = seen.single()
            context.block shouldBe Block.STONE
            context.instance shouldBe instance
            context.blockFace shouldBe BlockFace.NORTH
            context.blockPosition shouldBe blockPosition
            context.cursorPosition shouldBe cursorPosition
            context.player shouldBe player
            context.hand shouldBe PlayerHand.OFF
        }
    }
}
