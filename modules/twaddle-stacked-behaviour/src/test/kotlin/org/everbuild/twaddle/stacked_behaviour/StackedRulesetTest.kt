package org.everbuild.twaddle.stacked_behaviour

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.tag.TagHandler
import org.everbuild.twaddle.stacked_behaviour.engine.loop.LoopingRuleChainApplicatorFactory

private class TestRuleset : StackedRuleset<TestRuleset, RulesetEditor<TestRuleset>>(emptyMap(), ::RulesetEditor)
private data class MessageContext(val message: String) : ActivityContext {
    private val tags = TagHandler.newHandler()
    override fun tagHandler(): TagHandler = tags
}
private class CountContext(var count: Int = 0) : ActivityContext {
    private val tags = TagHandler.newHandler()
    override fun tagHandler(): TagHandler = tags
}
private val messageActivity = ActivityKey<TestRuleset, MessageContext>("test:message")
private val countActivity = ActivityKey<TestRuleset, CountContext>("test:count")
private val subject = Key.key("test:subject")
private val factory = LoopingRuleChainApplicatorFactory()
private fun increment(name: String, by: Int) = Rule<CountContext>(Rule.Key("test", name)) {
    it.count += by
    RuleResult.Next
}

val StackedRulesetTests by testSuite {
    test("Configurations can be read replaced and removed without changing earlier snapshots") {
        val editor = BehaviourEditor<TestRuleset>()
        editor.configuration(String::class.java) shouldBe null
        editor.configuration(String::class.java, "first")
        editor.configuration(CharSequence::class.java, "declared type")
        val original = editor.build()
        editor.configuration(String::class.java, "second")
        editor.configuration(String::class.java) shouldBe "second"
        val replaced = editor.build()
        editor.removeConfiguration(String::class.java)
        editor.removeConfiguration(String::class.java)
        editor.build().configuration(String::class.java) shouldBe null
        original.configuration<String>() shouldBe "first"
        original.configuration(CharSequence::class.java) shouldBe "declared type"
        replaced.configuration(String::class.java) shouldBe "second"
        val modified = original.modify {
            configuration(String::class.java) shouldBe "first"
            removeConfiguration(String::class.java)
        }
        modified.configuration(String::class.java) shouldBe null
        original.configuration(String::class.java) shouldBe "first"
    }

    test("Rulesets combine disjoint configuration classes and reject repeated classes") {
        val configured = StackedRuleset.build<TestRuleset> {
            add(subject) { configuration(String::class.java, "message") }
        }
        val other = StackedRuleset.build<TestRuleset> {
            add(subject) {
                configuration(List::class.java, listOf("entry"))
                activity(countActivity).append { install(increment("configured", 2)) }
            }
        }
        val combined = StackedRuleset.combine(configured, other)[subject]!!
        combined.configuration(String::class.java) shouldBe "message"
        combined.configuration(List::class.java) shouldBe listOf("entry")
        combined.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 2
        shouldThrow<IllegalArgumentException> { StackedRuleset.combine(configured, configured) }
        val removed = configured.modify { behaviours(subject) { removeConfiguration(String::class.java) } }
        StackedRuleset.combine(configured, removed)[subject]!!.configuration(String::class.java) shouldBe "message"
    }

    test("Retaining subjects filters without reordering or changing their behaviours") {
        val second = Key.key("test:second")
        val third = Key.key("test:third")
        val missing = Key.key("test:missing")
        val original = StackedRuleset.build<TestRuleset> {
            add(subject) { activity(countActivity).append { install(increment("count", 3)) } }
            add(second) {}
            add(third) { activity(messageActivity).clear() }
        }
        val selected = original.modify { retainOnly(third, missing, subject, subject) }
        selected.behaviours.keys.toList() shouldBe listOf(subject, third)
        selected[subject] shouldBe original[subject]
        selected[third] shouldBe original[third]
        selected[subject]!!.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 3
        original.behaviours.keys.toList() shouldBe listOf(subject, second, third)
        original.modify { retainOnly() }.behaviours shouldBe emptyMap()
        original.modify { retainOnly(missing) }.behaviours shouldBe emptyMap()
    }

    test("One behaviour dispatches different typed activities and retains repeated edits") {
        val ruleset = StackedRuleset.build<TestRuleset> {
            add(subject) {
                activity(messageActivity).replace {
                    install(Rule(Rule.Key("test:uppercase")) { RuleResult.cascade(MessageContext(it.message.uppercase())) })
                }
                activity(countActivity).append { install(increment("first", 1)) }
                activity(countActivity).append { install(increment("second", 2)) }
            }
        }
        val behaviour = ruleset[subject]!!
        behaviour.createApplicator(messageActivity, factory).apply(MessageContext("hello")).finalContext.message shouldBe "HELLO"
        behaviour.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 3
        val modified = behaviour.modify { activity(countActivity).omit() }
        modified.activity(countActivity) shouldBe null
        modified.createApplicator(messageActivity, factory).apply(MessageContext("hello")).finalContext.message shouldBe "HELLO"
        behaviour.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 3
        val absent = ActivityKey<TestRuleset, CountContext>("test:absent")
        behaviour.activity(absent) shouldBe null
        behaviour.createApplicator(absent, factory).apply(CountContext()).ruleResult shouldBe RuleResult.Next
    }

    test("Combining rulesets accepts different activities but rejects overlapping empty contributions") {
        val messages = StackedRuleset.build<TestRuleset> { add(subject) { activity(messageActivity).clear() } }
        val counts = StackedRuleset.build<TestRuleset> { add(subject) { activity(countActivity).append { install(increment("count", 4)) } } }
        val combined = StackedRuleset.combine(messages, counts)
        combined[subject]!!.activity(messageActivity)!!.let { factory.create(it).apply(MessageContext("same")) }.finalContext.message shouldBe "same"
        combined[subject]!!.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 4
        shouldThrow<IllegalArgumentException> { StackedRuleset.combine(messages, messages) }
        val omitted = messages.modify { behaviours(subject) { activity(messageActivity).omit() } }
        omitted[subject]!!.activity(messageActivity) shouldBe null
        StackedRuleset.combine(messages, omitted)[subject]!!.activity(messageActivity)!!.let {
            factory.create(it).apply(MessageContext("same")).ruleResult shouldBe RuleResult.Next
        }
    }

    test("Modifications snapshot editors and commit selected behaviours atomically") {
        val other = Key.key("test:other")
        val missing = Key.key("test:missing")
        lateinit var retained: BehaviourEditor<TestRuleset>
        val original = StackedRuleset.build<TestRuleset> {
            add(subject) { retained = this; activity(countActivity).append { install(increment("first", 1)) } }
            add(other) { activity(countActivity).append { install(increment("second", 2)) } }
        }
        retained.activity(countActivity).clear()
        original[subject]!!.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 1
        var edits = 0
        val modified = original.modify {
            shouldThrow<IllegalArgumentException> { add(subject) {} }
            shouldThrow<IllegalArgumentException> { behaviours(subject, missing) { activity(countActivity).clear() } }
            shouldThrow<IllegalArgumentException> { remove(subject, missing) }
            shouldThrow<IllegalArgumentException> {
                behaviours(subject, other) {
                    activity(countActivity).append { install(increment("added", 10)) }
                    require(++edits < 2)
                }
            }
            behaviours(subject, other) { activity(countActivity).append { install(increment("added", 3)) } }
        }
        original[subject]!!.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 1
        modified[subject]!!.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 4
        modified[other]!!.createApplicator(countActivity, factory).apply(CountContext()).finalContext.count shouldBe 5
        modified.modify { remove(subject) }[subject] shouldBe null
    }

    test("Keys with the same textual identifier cannot mix different context types") {
        val messages = ActivityKey<TestRuleset, MessageContext>("test:shared")
        val counts = ActivityKey<TestRuleset, CountContext>("test:shared")
        val ruleset = StackedRuleset.build<TestRuleset> {
            add(subject) {
                activity(messages).clear()
                activity(counts).append { install(increment("count", 7)) }
            }
        }
        ruleset[subject]!!.createApplicator(messages, factory).apply(MessageContext("message")).finalContext.message shouldBe "message"
        ruleset[subject]!!.createApplicator(counts, factory).apply(CountContext()).finalContext.count shouldBe 7
    }
}
