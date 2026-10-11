package org.everbuild.twaddle.stacked_behaviour_block

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.common.block.tick.RandomTickHandler
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.configuration.blockEntity
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.RandomTickActivityContext
import org.everbuild.twaddle.stacked_behaviour_block.type.randomTick
import org.everbuild.twaddle.stacked_behaviour_block.type.tick

private fun BlockRuleset.randomTickTestHandler(): BlockHandler =
    createHandlers(LoopingRuleChainApplicatorFactory(), RuleBlockHandlerFactory) { it }.getValue(Block.STONE.key())

val RandomTickRuleBlockHandlerTests by testSuite {
    envTest("Random tick contributions enable the interface independently of regular ticking") { env ->
        val instance = env.createEmptyInstance()
        val event = RandomTickHandler.RandomTick(Block.STONE, instance, BlockVec(1, 2, 3))
        val absent = BlockRuleset.build { addBlock(Block.STONE) {} }
        (absent.randomTickTestHandler() is RandomTickHandler) shouldBe false

        val configured = absent.modify {
            blocks(Block.STONE) {
                randomTick.append {
                    install(Rule(Rule.Key("test:random_tick")) { RuleResult.Next })
                }
            }
        }
        val handler = configured.randomTickTestHandler()
        handler.shouldBeInstanceOf<RandomTickHandler>().onRandomTick(event) shouldBe null
        handler.isTickable shouldBe false

        val cleared = configured.modify { blocks(Block.STONE) { randomTick.clear() } }
        cleared.randomTickTestHandler().shouldBeInstanceOf<RandomTickHandler>()
            .onRandomTick(event) shouldBe null
        val omitted = configured.modify { blocks(Block.STONE) { randomTick.omit() } }
        (omitted.randomTickTestHandler() is RandomTickHandler) shouldBe false
        (configured.randomTickTestHandler() is RandomTickHandler) shouldBe true

        val regularOnly = absent.modify { blocks(Block.STONE) { tick.clear() } }.randomTickTestHandler()
        regularOnly.isTickable shouldBe true
        (regularOnly is RandomTickHandler) shouldBe false
        val both = configured.modify { blocks(Block.STONE) { tick.clear() } }.randomTickTestHandler()
        both.isTickable shouldBe true
        both.shouldBeInstanceOf<RandomTickHandler>().onRandomTick(event) shouldBe null
    }

    envTest("Random tick replacements survive cascading and are returned without applying them to the instance") { env ->
        val instance = env.createEmptyInstance()
        val position = BlockVec(-17, 4, 33)
        instance.loadChunk(position.chunkX(), position.chunkZ()).join()
        instance.setBlock(position, Block.STONE)
        val seen = mutableListOf<RandomTickActivityContext>()
        val laterBlocks = mutableListOf<Block>()
        val clientTag = Tag.String("test:client_data")
        val ruleset = BlockRuleset.build {
            addBlock(Block.STONE) {
                blockEntity {
                    action = 1
                    tags += clientTag
                }
                randomTick.append {
                    install(
                        Rule(Rule.Key("test:replace_random_tick")) {
                            it.newBlock shouldBe null
                            seen += it
                            it.replaceBlock(Block.DIRT)
                            RuleResult.cascade(it.copy())
                        },
                        Rule(Rule.Key("test:observe_random_tick")) {
                            laterBlocks += it.block
                            it.newBlock shouldBe Block.DIRT
                            RuleResult.Consume
                        },
                    )
                }
            }
        }
        val handler = ruleset.randomTickTestHandler()
        handler.key shouldBe Block.STONE.key()
        handler.blockEntityAction shouldBe 1.toByte()
        handler.blockEntityTags.toSet() shouldBe setOf(clientTag)
        val randomHandler = handler.shouldBeInstanceOf<RandomTickHandler>()
        randomHandler.shouldRandomTick(Block.STONE) shouldBe true
        repeat(2) {
            randomHandler.onRandomTick(RandomTickHandler.RandomTick(Block.STONE, instance, position)) shouldBe Block.DIRT
            instance.getBlock(position) shouldBe Block.STONE
        }
        seen.size shouldBe 2
        seen.forEach {
            it.block shouldBe Block.STONE
            it.instance shouldBe instance
            it.blockPosition shouldBe position
        }
        (seen[0].tagHandler() === seen[1].tagHandler()) shouldBe false
        laterBlocks shouldBe listOf(Block.STONE, Block.STONE)
    }
}
