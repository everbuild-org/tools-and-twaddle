package org.everbuild.twaddle.stacked_behaviour_block

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.TickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.PlacementActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.DestroyActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.TouchActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.place
import org.everbuild.twaddle.stacked_behaviour_block.type.destroy
import org.everbuild.twaddle.stacked_behaviour_block.type.touch
import org.everbuild.twaddle.stacked_behaviour_block.type.tick

private fun BlockRuleset.stoneHandler(): BlockHandler =
    createHandlers(LoopingRuleChainApplicatorFactory(), RuleBlockHandlerFactory) { it }.getValue(Block.STONE.key())

val BlockHandlerActivitiesTests by testSuite {
    envTest("Placement activity preserves both programmatic and player placement details") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        val position = Vec(4.0, 5.0, 6.0)
        val seen = mutableListOf<PlacementActivityContext>()
        val ruleset = BlockRuleset.build {
            addBlock(Block.STONE) {
                place.append {
                    install(Rule(Rule.Key("test:place")) { seen += it; RuleResult.Next })
                }
            }
        }
        val handler = ruleset.stoneHandler()
        handler.onPlace(BlockHandler.Placement(Block.STONE, Block.DIRT, instance, position))
        handler.onPlace(BlockHandler.PlayerPlacement(
            Block.STONE, Block.DIRT, instance, position, player, PlayerHand.OFF, BlockFace.NORTH,
            0.25f, 0.5f, 0.75f,
        ))
        seen.size shouldBe 2
        seen.forEach {
            it.block shouldBe Block.STONE
            it.previousBlock shouldBe Block.DIRT
            it.instance shouldBe instance
            it.blockPosition shouldBe position
        }
        val automatic = seen.first()
        automatic.player shouldBe null
        automatic.hand shouldBe null
        automatic.blockFace shouldBe null
        automatic.cursorX shouldBe null
        automatic.cursorY shouldBe null
        automatic.cursorZ shouldBe null
        val placed = seen.last()
        placed.player shouldBe player
        placed.hand shouldBe PlayerHand.OFF
        placed.blockFace shouldBe BlockFace.NORTH
        placed.cursorX shouldBe 0.25f
        placed.cursorY shouldBe 0.5f
        placed.cursorZ shouldBe 0.75f
        BlockRuleset.build { addBlock(Block.STONE) {} }.stoneHandler()
            .onPlace(BlockHandler.Placement(Block.STONE, Block.DIRT, instance, position))
        seen.size shouldBe 2
    }

    envTest("Destroy activity preserves replacement and player details") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        val position = Vec(4.0, 5.0, 6.0)
        val seen = mutableListOf<DestroyActivityContext>()
        val ruleset = BlockRuleset.build {
            addBlock(Block.STONE) {
                destroy.append {
                    install(Rule(Rule.Key("test:destroy")) { seen += it; RuleResult.Next })
                }
            }
        }
        val handler = ruleset.stoneHandler()
        handler.onDestroy(BlockHandler.Destroy(Block.STONE, Block.AIR, instance, position))
        handler.onDestroy(BlockHandler.PlayerDestroy(Block.STONE, Block.AIR, instance, position, player))
        seen.size shouldBe 2
        seen.forEach {
            it.block shouldBe Block.STONE
            it.newBlock shouldBe Block.AIR
            it.instance shouldBe instance
            it.blockPosition shouldBe position
        }
        seen.first().player shouldBe null
        seen.last().player shouldBe player
        BlockRuleset.build { addBlock(Block.STONE) {} }.stoneHandler()
            .onDestroy(BlockHandler.Destroy(Block.STONE, Block.AIR, instance, position))
        seen.size shouldBe 2
    }

    envTest("Touch activity preserves the touching entity and consume stops its remaining rules") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        val position = Vec(4.0, 5.0, 6.0)
        val seen = mutableListOf<TouchActivityContext>()
        var laterRuleRan = false
        val ruleset = BlockRuleset.build {
            addBlock(Block.STONE) {
                touch.append {
                    install(
                        Rule(Rule.Key("test:touch")) { seen += it; RuleResult.Consume },
                        Rule(Rule.Key("test:later_touch")) { laterRuleRan = true; RuleResult.Next },
                    )
                }
            }
        }
        val event = BlockHandler.Touch(Block.STONE, instance, position, player)
        ruleset.stoneHandler().onTouch(event)
        seen.size shouldBe 1
        seen.single().block shouldBe Block.STONE
        seen.single().instance shouldBe instance
        seen.single().blockPosition shouldBe position
        seen.single().touching shouldBe player
        laterRuleRan shouldBe false
        val omitted = ruleset.modify { blocks(Block.STONE) { touch.omit() } }
        omitted.stoneHandler().onTouch(event)
        seen.size shouldBe 1
    }

    envTest("Tick contributions enable ticking and dispatch fresh contexts while omission disables ticking") { env ->
        val instance = env.createEmptyInstance()
        val position = Vec(2.0, 3.0, 4.0)
        val seen = mutableListOf<TickActivityContext>()
        val absent = BlockRuleset.build { addBlock(Block.STONE) {} }
        absent.stoneHandler().isTickable shouldBe false
        absent.stoneHandler().tick(BlockHandler.Tick(Block.STONE, instance, position))

        val configured = absent.modify {
            blocks(Block.STONE) {
                tick.append {
                    install(Rule(Rule.Key("test:tick")) {
                        seen += it
                        RuleResult.Next
                    })
                }
            }
        }
        val handler = configured.stoneHandler()
        handler.isTickable shouldBe true
        repeat(2) { handler.tick(BlockHandler.Tick(Block.STONE, instance, position)) }
        seen.size shouldBe 2
        seen.forEach {
            it.block shouldBe Block.STONE
            it.instance shouldBe instance
            it.blockPosition shouldBe position
        }
        (seen[0].tagHandler() === seen[1].tagHandler()) shouldBe false

        val cleared = configured.modify { blocks(Block.STONE) { tick.clear() } }
        cleared.stoneHandler().isTickable shouldBe true
        cleared.stoneHandler().tick(BlockHandler.Tick(Block.STONE, instance, position))
        val omitted = configured.modify { blocks(Block.STONE) { tick.omit() } }
        omitted.stoneHandler().isTickable shouldBe false
        omitted.stoneHandler().tick(BlockHandler.Tick(Block.STONE, instance, position))
        seen.size shouldBe 2
        configured.stoneHandler().isTickable shouldBe true
    }
}
