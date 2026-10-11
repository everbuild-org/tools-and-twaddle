package org.everbuild.twaddle.stacked_behaviour_block

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.stacked_behaviour.Rule
import org.everbuild.twaddle.stacked_behaviour.RuleChain
import org.everbuild.twaddle.stacked_behaviour.RuleResult
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_behaviour_block.mock.mock
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivity
import org.everbuild.twaddle.stacked_behaviour_block.type.InteractionActivityContext
import org.mockito.kotlin.mock

private val callsTag = Tag.String("test:calls").list()

private fun RuleChain<InteractionActivityContext>.executionOrder(): List<String> {
    val context = InteractionActivityContext.mock(mock(), mock())
    val tags = context.tagHandler()
    tags.setTag(callsTag, emptyList())
    LoopingRuleChainApplicatorFactory().create(this).apply(context)
    return tags.getTag(callsTag)
}

private fun interactionRule(name: String): Rule<InteractionActivityContext> =
    Rule(Rule.Key("test", name)) {
        val tags = it.tagHandler()
        tags.setTag(callsTag, tags.getTag(callsTag) + name)
        RuleResult.Next
    }

val BlockRulesetTests by testSuite {
    envTest("Modifying selected blocks preserves the original and edits each chain independently") {
        val stone = Key.key("minecraft:stone")
        val dirt = Key.key("minecraft:dirt")
        val first = interactionRule("first")
        val second = interactionRule("second")
        val added = interactionRule("added")
        val original = BlockRuleset.build {
            addBlock(stone) { activity(InteractionActivity).replace { install(first) } }
            addBlock(dirt) { activity(InteractionActivity).replace { install(second) } }
        }
        val modified = original.modify {
            blocks(stone, dirt) { activity(InteractionActivity).append { install(added) } }
        }
        original[stone]!!.activity(InteractionActivity)!!.executionOrder() shouldBe listOf("first")
        original[dirt]!!.activity(InteractionActivity)!!.executionOrder() shouldBe listOf("second")
        modified[stone]!!.activity(InteractionActivity)!!.executionOrder() shouldBe listOf("first", "added")
        modified[dirt]!!.activity(InteractionActivity)!!.executionOrder() shouldBe listOf("second", "added")
    }

    envTest("Invalid block selections fail and leave existing entries unchanged") {
        val present = Key.key("test:present")
        val missing = Key.key("test:missing")
        val rule = interactionRule("original")
        val original =
            BlockRuleset.build { addBlock(present) { activity(InteractionActivity).replace { install(rule) } } }
        val unchanged = original.modify {
            shouldThrow<IllegalArgumentException> { addBlock(present) {} }
            shouldThrow<IllegalArgumentException> { blocks(present, missing) { activity(InteractionActivity).clear() } }
            shouldThrow<IllegalArgumentException> { removeBlocks(present, missing) }
        }
        unchanged[present]!!.activity(InteractionActivity)!!.executionOrder() shouldBe listOf("original")
        unchanged[missing] shouldBe null
    }

    envTest("Absent and explicitly empty contributions have different combination semantics") {
        val key = Key.key("test:block")
        val absent = BlockRuleset.build { addBlock(key) {} }
        val empty = absent.modify { blocks(key) { activity(InteractionActivity).clear() } }
        absent[key]!!.activity(InteractionActivity) shouldBe null
        empty[key]!!.activity(InteractionActivity)!!.executionOrder() shouldBe emptyList()
        BlockRuleset.combine(absent, empty)[key]!!.activity(InteractionActivity)!!.executionOrder() shouldBe emptyList()
        BlockRuleset.combine(empty, absent)[key]!!.activity(InteractionActivity)!!.executionOrder() shouldBe emptyList()
        shouldThrow<IllegalArgumentException> { BlockRuleset.combine(empty, empty) }
        empty.modify { blocks(key) { activity(InteractionActivity).omit() } }[key]!!.activity(InteractionActivity) shouldBe null
    }

    envTest("Disjoint block contributions combine while overlapping interaction contributions fail") {
        val firstKey = Key.key("test:first")
        val secondKey = Key.key("test:second")
        val rule = interactionRule("rule")
        val first =
            BlockRuleset.build { addBlock(firstKey) { activity(InteractionActivity).replace { install(rule) } } }
        val second =
            BlockRuleset.build { addBlock(secondKey) { activity(InteractionActivity).replace { install(rule) } } }
        val combined = BlockRuleset.combine(first, second)
        combined[firstKey]!!.activity(InteractionActivity)!!.executionOrder() shouldBe listOf("rule")
        combined[secondKey]!!.activity(InteractionActivity)!!.executionOrder() shouldBe listOf("rule")
        shouldThrow<IllegalArgumentException> { BlockRuleset.combine(first, first) }
    }
}

