package org.everbuild.twaddle.stacked_behaviour

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.assertions.withClue
import io.kotest.matchers.shouldBe
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory

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
