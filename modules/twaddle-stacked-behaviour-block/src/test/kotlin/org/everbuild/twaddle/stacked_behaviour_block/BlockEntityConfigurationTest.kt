package org.everbuild.twaddle.stacked_behaviour_block

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.minestom.server.coordinate.Pos
import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import net.minestom.server.instance.block.BlockHandler
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.configuration
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.configuration.BlockEntityConfiguration
import org.everbuild.twaddle.stacked_behaviour_block.configuration.BlockEntityConfigurationEditor
import org.everbuild.twaddle.stacked_behaviour_block.configuration.blockEntity
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.interact

val BlockEntityConfigurationTests by testSuite {
    envTest("Block entity configuration reaches handlers alongside interaction rules") { env ->
        val inventoryTag = Tag.String("inventory")
        val ruleset = BlockRuleset.build {
            blocks(Block.CHEST) {
                blockEntity {
                    action = 1
                    tags += inventoryTag
                }
                interact.append {
                    install(Rule(Rule.Key("test:open_chest")) { RuleResult.Consume })
                }
            }
        }
        val handler = ruleset.createHandlers(LoopingRuleChainApplicatorFactory(), RuleBlockHandlerFactory) { it }
            .getValue(Block.CHEST.key())
        handler.blockEntityAction shouldBe 1.toByte()
        handler.blockEntityTags.toSet() shouldBe setOf(inventoryTag)
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        handler.onInteract(BlockHandler.Interaction(
            Block.CHEST, instance, BlockFace.TOP, Vec.ZERO, Vec.ZERO, player, PlayerHand.MAIN,
        )) shouldBe false

        val removed = ruleset.modify {
            blocks(Block.CHEST) { removeConfiguration(BlockEntityConfiguration::class.java) }
        }
        val defaultHandler = removed.createHandlers(LoopingRuleChainApplicatorFactory(), RuleBlockHandlerFactory) { it }
            .getValue(Block.CHEST.key())
        defaultHandler.blockEntityAction shouldBe (-1).toByte()
        defaultHandler.blockEntityTags shouldBe emptySet()
        handler.blockEntityAction shouldBe 1.toByte()
    }

    envTest("Block entity edits preserve existing settings and snapshot mutable tag selections") { _ ->
        val first = Tag.String("first")
        val second = Tag.String("second")
        val input = mutableSetOf<Tag<*>>(first)
        val configuration = BlockEntityConfiguration(1, input)
        input.clear()
        configuration.tags shouldBe setOf(first)
        lateinit var retained: BlockEntityConfigurationEditor
        val original = BlockRuleset.build {
            blocks(Block.CHEST) { configuration(BlockEntityConfiguration::class.java, configuration) }
        }
        val modified = original.modify {
            blocks(Block.CHEST) {
                blockEntity {
                    retained = this
                    tags += second
                    tags += first
                }
                shouldThrow<IllegalStateException> {
                    blockEntity {
                        action = 2
                        tags.clear()
                        error("abort edit")
                    }
                }
            }
        }
        retained.tags.clear()
        retained.action = 3
        original[Block.CHEST.key()]!!.configuration<BlockEntityConfiguration>()!!.tags shouldBe setOf(first)
        val snapshot = modified[Block.CHEST.key()]!!.configuration<BlockEntityConfiguration>()!!
        snapshot.action shouldBe 1.toByte()
        snapshot.tags shouldBe setOf(first, second)
    }
}
