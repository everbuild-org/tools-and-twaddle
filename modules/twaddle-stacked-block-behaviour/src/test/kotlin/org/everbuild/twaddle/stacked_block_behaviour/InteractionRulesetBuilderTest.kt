package org.everbuild.twaddle.stacked_block_behaviour

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.minestom.server.coordinate.Pos
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.core.test.shouldBeCalledAtMost
import org.everbuild.twaddle.stacked_block_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_block_behaviour.mock.cancelSet
import org.everbuild.twaddle.stacked_block_behaviour.mock.mock
import org.everbuild.twaddle.stacked_block_behaviour.mock.mutatePipeline
import org.everbuild.twaddle.stacked_block_behaviour.mock.record
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.type.createApplicator

val InteractionRulesetBuilderTests by testSuite {
    envTest("Empty rulechain allows further processing") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)

        val looping = LoopingRuleChainApplicatorFactory()

        val applicator = RuleChain.build<InteractionActivityContext> {}.createApplicator(looping)
        val result = applicator.apply(InteractionActivityContext.mock(instance, player))

        result.ruleResult shouldBe RuleResult.next()
    }

    envTest("Cancelling Rulechain stops further processing") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        val looping = LoopingRuleChainApplicatorFactory()
        val callback = shouldBeCalledAtMost(0)

        val applicator = RuleChain.build {
            cancelSet()
            record { callback() }
        }.createApplicator(looping)

        val result = applicator.apply(InteractionActivityContext.mock(instance, player))
        result.ruleResult shouldBe RuleResult.consume()
    }

    envTest("Modification should persist to later pipeline stages") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)
        val looping = LoopingRuleChainApplicatorFactory()

        val applicator = RuleChain.build<InteractionActivityContext> {
            record { it.block shouldBe Block.STONE }
            mutatePipeline(Block.DIAMOND_BLOCK)
            record { it.block shouldBe Block.DIAMOND_BLOCK }
        }.createApplicator(looping)

        val result = applicator.apply(InteractionActivityContext.mock(instance, player))
        result.ruleResult shouldBe RuleResult.next()
    }
}