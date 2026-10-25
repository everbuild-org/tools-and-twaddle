package org.everbuild.twaddle.loot

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.withRandomSequenceController
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.loot.entry.LootEntry
import org.everbuild.twaddle.loot.entry.createVanillaLootEntryTypeRegistry
import org.everbuild.twaddle.loot.withDynamicLootResolver
import org.everbuild.twaddle.loot.withLootLuck
import org.everbuild.twaddle.loot.entry.impl.AlternativesLootEntryData
import org.everbuild.twaddle.loot.entry.impl.EmptyLootEntryData
import org.everbuild.twaddle.loot.entry.impl.ItemLootEntryData
import org.everbuild.twaddle.loot.entry.impl.LootTableReferenceEntryData
import org.everbuild.twaddle.loot.entry.impl.LootTableEntryValue
import org.everbuild.twaddle.loot.entry.impl.SequenceLootEntryData
import org.everbuild.twaddle.loot.entry.impl.DynamicLootEntryData
import org.everbuild.twaddle.loot.entry.impl.SlotsLootEntryData
import org.everbuild.twaddle.loot.entry.impl.TagLootEntryData
import org.everbuild.twaddle.loot.modifier.createVanillaLootModifierTypeRegistry
import org.everbuild.twaddle.loot.modifier.impl.SetCountLootModifier
import java.util.Random

private data class FixedIntProvider(val value: Int) : NumberProviderDeclaration<Int> {
    override fun type() = Key.key("loot_test:fixed_int")
    override fun get(context: DataContext, random: Random) = value
}

private data class FixedFloatProvider(val value: Float) : NumberProviderDeclaration<Float> {
    override fun type() = Key.key("loot_test:fixed_float")
    override fun get(context: DataContext, random: Random) = value
}

private data class TestPredicate(val result: Boolean) : ContextPredicate {
    override fun type() = Key.key("loot_test:predicate")
    override fun test(context: DataContext, random: Random) = result
}

private val evaluator = LootTableEvaluator(
    createVanillaLootEntryTypeRegistry(),
    createVanillaLootModifierTypeRegistry(),
)

private fun entry(data: org.everbuild.twaddle.loot.entry.LootEntryData, vararg predicates: ContextPredicate) =
    LootEntry(value = data, predicates = predicates.toList())

private fun table(vararg entries: LootEntry, rolls: Int = 1) = LootTable(
    pools = listOf(LootPool(FixedIntProvider(rolls), entries.toList())),
)

private fun context() = DataContext(hashMapOf())

val lootTableEvaluatorTests by testSuite("Loot table evaluator") {
    test("generates the selected item once for every pool roll") {
        evaluator.generate(
            table(entry(ItemLootEntryData(Key.key("minecraft:stick"))), rolls = 3),
            context(),
            Random(1),
        ) shouldContainExactly List(3) { ItemStack.of(Material.STICK) }
    }

    test("entry and pool predicates gate generation") {
        val pool = LootPool(
            rolls = FixedIntProvider(1),
            entries = listOf(entry(ItemLootEntryData(Key.key("minecraft:stick")), TestPredicate(false))),
            predicates = listOf(TestPredicate(true)),
        )
        evaluator.generate(LootTable(pools = listOf(pool)), context(), Random(2)) shouldBe emptyList()

        val rejectedPool = pool.copy(predicates = listOf(TestPredicate(false)))
        evaluator.generate(LootTable(pools = listOf(rejectedPool)), context(), Random(2)) shouldBe emptyList()
    }

    test("bonus rolls and quality adjusted weights use context luck") {
        val bonusPool = LootPool(
            rolls = FixedIntProvider(1),
            entries = listOf(entry(ItemLootEntryData(Key.key("minecraft:stick")))),
            bonusRolls = FixedFloatProvider(0.75f),
        )
        evaluator.generate(LootTable(pools = listOf(bonusPool)), context().withLootLuck(2f), Random(21)) shouldBe
            List(2) { ItemStack.of(Material.STICK) }

        val qualityTable = table(
            entry(ItemLootEntryData(Key.key("minecraft:stick"))),
            entry(ItemLootEntryData(Key.key("minecraft:diamond"))).copy(quality = 1),
        )
        val pick = Random(22).nextLong(3)
        val expected = if (pick == 0L) Material.STICK else Material.DIAMOND
        evaluator.generate(qualityTable, context().withLootLuck(1f), Random(22)) shouldBe
            listOf(ItemStack.of(expected))
    }

    test("alternatives choose the first child whose conditions pass") {
        val alternatives = entry(
            AlternativesLootEntryData(
                listOf(
                    entry(ItemLootEntryData(Key.key("minecraft:diamond")), TestPredicate(false)),
                    entry(ItemLootEntryData(Key.key("minecraft:stick")), TestPredicate(true)),
                ),
            ),
        )

        evaluator.generate(table(alternatives), context(), Random(3)) shouldBe
            listOf(ItemStack.of(Material.STICK))
    }

    test("sequences stop at a failed child while retaining earlier successful children") {
        val sequence = entry(
            SequenceLootEntryData(
                listOf(
                    entry(ItemLootEntryData(Key.key("minecraft:stick"))),
                    entry(EmptyLootEntryData, TestPredicate(false)),
                    entry(ItemLootEntryData(Key.key("minecraft:diamond"))),
                ),
            ),
        )

        evaluator.generate(table(sequence), context(), Random(31)) shouldBe
            listOf(ItemStack.of(Material.STICK))
    }

    test("tag entries either drop the whole tag or select an expanded item") {
        val tag = TagLootEntryData(Key.key("minecraft:planks"))
        val allTagItems = evaluator.generate(table(entry(tag)), context(), Random(32))
        allTagItems shouldContainExactly tag.resolveItems().map(ItemStack::of)

        val expanded = entry(tag.copy(expand = true)).copy(
            modifiers = listOf(SetCountLootModifier(FixedIntProvider(8))),
        )
        val selected = evaluator.generate(table(expanded), context(), Random(33)).single()
        (selected.material() in tag.resolveItems()) shouldBe true
        selected.amount() shouldBe 1
    }

    test("entry, pool, and table modifiers run in order") {
        val lootTable = LootTable(
            pools = listOf(
                LootPool(
                    rolls = FixedIntProvider(1),
                    entries = listOf(
                        entry(ItemLootEntryData(Key.key("minecraft:stick"))).copy(
                            modifiers = listOf(SetCountLootModifier(FixedIntProvider(3), add = true)),
                        ),
                    ),
                    modifiers = listOf(SetCountLootModifier(FixedIntProvider(4))),
                ),
            ),
            modifiers = listOf(SetCountLootModifier(FixedIntProvider(2), add = true)),
        )

        evaluator.generate(lootTable, context(), Random(4)) shouldBe listOf(ItemStack.of(Material.STICK, 6))
    }

    test("nested table entries resolve through DataContext") {
        val reference = entry(LootTableReferenceEntryData(Key.key("loot_test:referenced")))
        val input = table(reference)
        val resolved = table(entry(ItemLootEntryData(Key.key("minecraft:diamond"))))
        val runtimeContext = context().withLootTableResolver { key ->
            key shouldBe Key.key("loot_test:referenced")
            resolved
        }

        evaluator.generate(input, runtimeContext, Random(5)) shouldBe listOf(ItemStack.of(Material.DIAMOND))
    }

    test("inline loot tables generate through the same evaluator") {
        val inline = LootTable(
            pools = listOf(
                LootPool(
                    rolls = FixedIntProvider(1),
                    entries = listOf(entry(ItemLootEntryData(Key.key("minecraft:diamond")))),
                ),
            ),
        )
        val inlineEntry = entry(LootTableReferenceEntryData(LootTableEntryValue.Inline(inline)))

        evaluator.generate(table(inlineEntry), context(), Random(53)) shouldBe
            listOf(ItemStack.of(Material.DIAMOND))
    }

    test("dynamic and slots entries use their host supplied data") {
        val dynamicName = Key.key("minecraft:contents")
        val dynamicContext = context().withDynamicLootResolver { name, _, _ ->
            name shouldBe dynamicName
            listOf(ItemStack.of(Material.DIAMOND))
        }
        evaluator.generate(table(entry(DynamicLootEntryData(dynamicName))), dynamicContext, Random(51)) shouldBe
            listOf(ItemStack.of(Material.DIAMOND))

        val slots = object : SlotSource {
            override fun type() = Key.key("loot_test:slots")
            override fun evaluate(context: DataContext) = listOf(ItemStack.AIR, ItemStack.of(Material.STICK))
        }
        evaluator.generate(table(entry(SlotsLootEntryData(slots))), context(), Random(52)) shouldBe
            listOf(ItemStack.of(Material.STICK))
    }

    test("missing table references and recursive references fail clearly") {
        val key = Key.key("loot_test:missing")
        val referenceTable = table(entry(LootTableReferenceEntryData(key)))
        shouldThrow<IllegalStateException> {
            evaluator.generate(referenceTable, context().withLootTableResolver { null }, Random(6))
        }
        var recursive = table(entry(LootTableReferenceEntryData(key)))
        shouldThrow<IllegalStateException> {
            evaluator.generate(recursive, context().withLootTableResolver { recursive }, Random(7))
        }
    }

    test("declared random sequences use the controller from DataContext") {
        val sequence = Key.key("loot_test:sequence")
        var calls = 0
        val random = Random(8)
        val runtimeContext = context().withRandomSequenceController { actual ->
            actual shouldBe sequence
            calls++
            random
        }

        evaluator.generate(table(entry(EmptyLootEntryData)).copy(randomSequence = sequence.asString()), runtimeContext)
        calls shouldBe 1
    }
}
