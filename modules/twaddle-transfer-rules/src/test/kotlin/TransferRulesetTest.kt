package org.everbuild.trwaddle.inventory_transfer

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

val transferRulesetTests by testSuite("Transfer ruleset") {
    test("Rules are evaluated from top to bottom") {
        val ruleset = transferRuleset {
            routeTo(listOf(3)) { true }
            blockAt(listOf(3)) { true }
            routeTo(listOf(3, 4)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(3),
            listOf(4),
        )
    }

    test("The first matching rule that mentions a slot claims it") {
        val ruleset = transferRuleset {
            routeTo(listOf(2, 1)) { true }
            routeTo(listOf(1, 3)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(2, 1),
            listOf(3),
        )
    }

    test("Each routing rule creates an ordered routing tier") {
        val ruleset = transferRuleset {
            routeTo(listOf(8, 6)) { true }
            routeTo(listOf(4, 2)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(8, 6),
            listOf(4, 2),
        )
    }

    test("A blocking rule keeps its slots out of every later tier") {
        val ruleset = transferRuleset {
            blockAt(listOf(2)) { true }
            routeTo(listOf(1, 2)) { true }
            routeTo(listOf(2, 3)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(1),
            listOf(3),
        )
    }

    test("A matching conditional route overrides a later unconditional block") {
        val ruleset = transferRuleset {
            routeTo(listOf(4)) { sourceSlot == 9 }
            blockAt(listOf(4)) { true }
            routeTo(listOf(4, 5)) { true }
        }

        ruleset.resolve(transferContext(sourceSlot = 9)) shouldBe listOf(
            listOf(4),
            listOf(5),
        )
    }

    test("A non-matching predicate does not claim its slots") {
        val ruleset = transferRuleset {
            routeTo(listOf(4)) { sourceSlot == 9 }
            routeTo(listOf(4)) { true }
        }

        ruleset.resolve(transferContext(sourceSlot = 1)) shouldBe listOf(listOf(4))
    }

    test("Duplicate slots are removed from later rules without reordering selectors") {
        val ruleset = transferRuleset {
            routeTo(listOf(5, 2, 5)) { true }
            routeTo(listOf(4, 2, 3, 5, 1)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe listOf(
            listOf(5, 2),
            listOf(4, 3, 1),
        )
    }

    test("SingleSlot resolves its one slot") {
        val ruleset = rulesetRoutingTo(SlotSelector.SingleSlot(7))

        ruleset.resolve(transferContext()) shouldBe listOf(listOf(7))
    }

    test("SlotRange resolves in its configured order") {
        val ascending = rulesetRoutingTo(
            SlotSelector.SlotRange(2, 5, ChooseSlotOrder.FIRST_TO_LAST)
        )
        val descending = rulesetRoutingTo(
            SlotSelector.SlotRange(2, 5, ChooseSlotOrder.LAST_TO_FIRST)
        )

        ascending.resolve(transferContext()) shouldBe listOf(listOf(2, 3, 4, 5))
        descending.resolve(transferContext()) shouldBe listOf(listOf(5, 4, 3, 2))
    }

    test("SlotProgression preserves direction and step") {
        val ruleset = rulesetRoutingTo(SlotSelector.SlotProgression(8 downTo 2 step 3))

        ruleset.resolve(transferContext()) shouldBe listOf(listOf(8, 5, 2))
    }

    test("SlotList preserves its declared order") {
        val ruleset = rulesetRoutingTo(SlotSelector.SlotList(intArrayOf(4, 1, 7)))

        ruleset.resolve(transferContext()) shouldBe listOf(listOf(4, 1, 7))
    }

    test("A built ruleset is unchanged when its builder is modified") {
        val builder = TransferRuleset.builder()
        builder.routeTo(1)
        val built = builder.build()

        builder.routeTo(2)

        built.resolve(transferContext()) shouldBe listOf(listOf(1))
        builder.build().resolve(transferContext()) shouldBe listOf(
            listOf(1),
            listOf(2),
        )
    }

    test("An empty ruleset produces no destinations") {
        val ruleset = TransferRuleset.builder().build()

        ruleset.resolve(transferContext()) shouldBe emptyList()
    }

    test("A fully blocked ruleset produces no destinations") {
        val ruleset = transferRuleset {
            blockAt(listOf(1, 2, 3)) { true }
        }

        ruleset.resolve(transferContext()) shouldBe emptyList()
    }
}

private fun rulesetRoutingTo(selector: SlotSelector): TransferRuleset =
    TransferRuleset.builder().apply {
        routeTo(selector)
    }.build()

private fun transferContext(sourceSlot: Int = 0): TransferContext = mock {
    on { this.sourceSlot } doReturn sourceSlot
}
