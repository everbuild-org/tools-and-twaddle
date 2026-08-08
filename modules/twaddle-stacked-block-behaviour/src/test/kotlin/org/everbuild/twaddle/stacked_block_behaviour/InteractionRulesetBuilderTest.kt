package org.everbuild.twaddle.stacked_block_behaviour

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.types.shouldBeSameInstanceAs
import net.minestom.server.coordinate.Pos
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.stacked_block_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_block_behaviour.mock.mock
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext
import org.everbuild.twaddle.stacked_block_behaviour.type.createApplicator

val InteractionRulesetBuilderTests by testSuite {
    envTest("Empty rulechain allows further processing") { env ->
        val instance = env.createEmptyInstance()
        val player = env.createPlayer(instance, Pos.ZERO)

        val looping = LoopingRuleChainApplicatorFactory()

        val applicator = RuleChain.build<InteractionActivityContext> {}.createApplicator(looping)
        val result = applicator.apply(InteractionActivityContext.mock(instance, player))

        result shouldBeSameInstanceAs RuleResult.next<InteractionActivityContext>()
    }
}