package org.everbuild.twaddle.stacked_behaviour_block

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
import org.everbuild.twaddle.stacked_behaviour.ActivityKey
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

val RuleBlockHandlerTests by testSuite {
    envTest("Custom activities coexist with interaction and absent interaction falls through") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        val customActivity = ActivityKey<BlockRuleset, InteractionActivityContext>("test:custom")
        val customRules = BlockRuleset.build {
            blocks(Block.STONE) {
                activity(customActivity).replace {
                    install(Rule(Rule.Key("test:custom_rule")) { RuleResult.consume() })
                }
            }
        }
        val interactionRules = BlockRuleset.build {
            blocks(Block.STONE) {
                activity(InteractionActivity).replace {
                    install(Rule(Rule.Key("test:interaction_rule")) { RuleResult.consume() })
                }
            }
        }
        val interaction = BlockHandler.Interaction(
            Block.STONE, instance, BlockFace.TOP, Vec.ZERO, Vec.ZERO, player, PlayerHand.MAIN,
        )
        val factory = LoopingRuleChainApplicatorFactory()
        customRules.createHandlers(factory, RuleBlockHandlerFactory) { it }
            .getValue(Block.STONE.key()).onInteract(interaction) shouldBe true
        val combined = BlockRuleset.combine(customRules, interactionRules)
        combined.createHandlers(factory, RuleBlockHandlerFactory) { it }
            .getValue(Block.STONE.key()).onInteract(interaction) shouldBe false
        combined[Block.STONE.key()]!!.createApplicator(customActivity, factory)
            .apply(InteractionActivityContext(interaction)).ruleResult shouldBe RuleResult.Consume
    }

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
                blocks(Block.STONE) {
                    interact.replace {
                        install(Rule(Rule.Key("test:outcome")) {
                            seen += it
                            outcome(it)
                        })
                    }
                }
            }
            val handlerKey = Key.key("test:stone_handler")
            val handler = ruleset.createHandlers(LoopingRuleChainApplicatorFactory(), RuleBlockHandlerFactory) { handlerKey }
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
