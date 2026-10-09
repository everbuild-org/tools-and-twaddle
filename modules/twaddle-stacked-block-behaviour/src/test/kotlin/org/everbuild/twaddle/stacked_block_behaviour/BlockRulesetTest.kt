package org.everbuild.twaddle.stacked_block_behaviour

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.withClue
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_block_behaviour.engine.loop.LoopingRuleChainApplicatorFactory
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext

private class RecordingContext : ActivityContext {
    val calls = mutableListOf<String>()
    private val tags = TagHandler.newHandler()
    override fun tagHandler(): TagHandler = tags
}

private fun recordingRule(name: String): Rule<RecordingContext> = Rule(Rule.Key("test", name)) {
    it.calls += name
    RuleResult.Next
}

private fun RuleChain<RecordingContext>.executionOrder(): List<String> {
    val context = RecordingContext()
    LoopingRuleChainApplicatorFactory().create(this).apply(context)
    return context.calls
}

private fun interactionRule(name: String): Rule<InteractionActivityContext> =
    Rule(Rule.Key("test", name)) { RuleResult.Next }

val BlockRulesetTests by testSuite {
    test("Modifying selected blocks preserves the original and edits each chain independently") {
        val stone = Key.key("minecraft:stone")
        val dirt = Key.key("minecraft:dirt")
        val first = interactionRule("first")
        val second = interactionRule("second")
        val added = interactionRule("added")
        val original = BlockRuleset.build {
            addBlock(stone) { interact.replace { install(first) } }
            addBlock(dirt) { interact.replace { install(second) } }
        }
        val modified = original.modify {
            blocks(stone, dirt) { interact.append { install(added) } }
        }
        original[stone]!!.interact!!.rules() shouldBe listOf(first)
        original[dirt]!!.interact!!.rules() shouldBe listOf(second)
        modified[stone]!!.interact!!.rules() shouldBe listOf(first, added)
        modified[dirt]!!.interact!!.rules() shouldBe listOf(second, added)
    }

    test("Invalid block selections fail and leave existing entries unchanged") {
        val present = Key.key("test:present")
        val missing = Key.key("test:missing")
        val rule = interactionRule("original")
        val original = BlockRuleset.build { addBlock(present) { interact.replace { install(rule) } } }
        val unchanged = original.modify {
            shouldThrow<IllegalArgumentException> { addBlock(present) {} }
            shouldThrow<IllegalArgumentException> { blocks(present, missing) { interact.clear() } }
            shouldThrow<IllegalArgumentException> { removeBlocks(present, missing) }
        }
        unchanged[present]!!.interact!!.rules() shouldBe listOf(rule)
        unchanged[missing] shouldBe null
    }

    test("Absent and explicitly empty contributions have different combination semantics") {
        val key = Key.key("test:block")
        val absent = BlockRuleset.build { addBlock(key) {} }
        val empty = absent.modify { blocks(key) { interact.clear() } }
        absent[key]!!.interact shouldBe null
        empty[key]!!.interact!!.rules() shouldBe emptyList()
        BlockRuleset.combine(absent, empty)[key]!!.interact!!.rules() shouldBe emptyList()
        BlockRuleset.combine(empty, absent)[key]!!.interact!!.rules() shouldBe emptyList()
        shouldThrow<IllegalArgumentException> { BlockRuleset.combine(empty, empty) }
        empty.modify { blocks(key) { interact.omit() } }[key]!!.interact shouldBe null
    }

    test("Disjoint block contributions combine while overlapping interaction contributions fail") {
        val firstKey = Key.key("test:first")
        val secondKey = Key.key("test:second")
        val rule = interactionRule("rule")
        val first = BlockRuleset.build { addBlock(firstKey) { interact.replace { install(rule) } } }
        val second = BlockRuleset.build { addBlock(secondKey) { interact.replace { install(rule) } } }
        val combined = BlockRuleset.combine(first, second)
        combined[firstKey]!!.interact!!.rules() shouldBe listOf(rule)
        combined[secondKey]!!.interact!!.rules() shouldBe listOf(rule)
        shouldThrow<IllegalArgumentException> { BlockRuleset.combine(first, first) }
    }
}

val RuleChainEditorTests by testSuite {
    test("Chain edits execute in the requested order and target equivalent keys") {
        val a = recordingRule("a")
        val b = recordingRule("b")
        val editor = RuleChainEditor<RecordingContext>(null)
        editor.replace { install(a) }
        editor.append { install(b) }
        editor.prepend { install(recordingRule("start")) }
        editor.before(recordingRule("b")) { install(recordingRule("before")) }
        editor.after(Rule.Key("test", "a")) { install(recordingRule("after")) }
        editor.build()!!.executionOrder() shouldBe listOf("start", "a", "after", "before", "b")
        editor.replace(recordingRule("a")) { install(recordingRule("replacement")) }
        editor.remove(recordingRule("b"))
        editor.build()!!.executionOrder() shouldBe listOf("start", "replacement", "after", "before")
        editor.replace { install(recordingRule("only")) }
        editor.build()!!.executionOrder() shouldBe listOf("only")
    }

    test("Missing targets and duplicate keys fail without changing the chain") {
        val a = recordingRule("a")
        val b = recordingRule("b")
        val editor = RuleChainEditor(RuleChain(listOf(a, b)))
        val missing = recordingRule("missing")
        val failures: List<Pair<String, () -> Unit>> = listOf(
            "before missing anchor" to { editor.before(missing) { install(recordingRule("new")) } },
            "after missing anchor" to { editor.after(missing) { install(recordingRule("new")) } },
            "replace missing target" to { editor.replace(missing) { install(recordingRule("new")) } },
            "remove missing target" to { editor.remove(missing) },
            "append duplicate key" to { editor.append { install(recordingRule("new")); install(recordingRule("a")) } },
            "prepend duplicate key" to { editor.prepend { install(recordingRule("b")) } },
            "before duplicate key" to { editor.before(b) { install(recordingRule("a")) } },
            "after duplicate key" to { editor.after(a) { install(recordingRule("b")) } },
            "targeted replacement duplicate key" to { editor.replace(a) { install(recordingRule("b")) } },
            "whole-chain replacement duplicate key" to { editor.replace { install(recordingRule("new")); install(recordingRule("new")) } },
        )
        failures.forEach { (name, operation) ->
            withClue(name) {
                shouldThrow<IllegalArgumentException> { operation() }
                editor.build()!!.executionOrder() shouldBe listOf("a", "b")
            }
        }
    }

    test("RuleChain snapshots the caller's mutable list") {
        val input = mutableListOf(recordingRule("original"))
        val chain = RuleChain(input)
        input.clear()
        input += recordingRule("later")
        chain.executionOrder() shouldBe listOf("original")
    }
}
