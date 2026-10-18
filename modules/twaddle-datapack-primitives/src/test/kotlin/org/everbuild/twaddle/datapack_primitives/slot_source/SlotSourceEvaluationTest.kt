package org.everbuild.twaddle.datapack_primitives.slot_source

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.withItemPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.withSlotSourceInventoryAccess
import org.everbuild.twaddle.core.test.envTest

private val slotSourceCodec = DatapackPrimitives.vanilla().slotSourceCodec

private fun slotSource(json: String): SlotSource =
    when (val decoded = slotSourceCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode slot source: ${decoded.message}")
    }

private fun item(material: Material) = ItemStack.of(material)

val slotSourceEvaluationTests by testSuite("Slot source evaluation") {
    test("empty selects no items") {
        slotSource("""{"type":"minecraft:empty"}""").evaluate(DataContext(hashMapOf())) shouldBe emptyList()
    }

    test("slot_range asks the context for its entity source and slot patterns") {
        val selected = listOf(item(Material.STONE), item(Material.DIAMOND))
        val result = slotSource(
            """{"type":"minecraft:slot_range","source":"attacking_entity","slots":"hotbar.*"}""",
        ).evaluate(
            DataContext(hashMapOf()).withSlotSourceInventoryAccess { source, pattern ->
                source shouldBe "attacking_entity"
                pattern shouldBe listOf("hotbar.*")
                selected
            },
        )

        result shouldContainExactly selected
    }

    test("slot_range preserves empty slots as air stacks") {
        val selected = listOf(item(Material.STONE), ItemStack.AIR, item(Material.DIAMOND))
        slotSource(
            """{"type":"minecraft:slot_range","source":"this","slots":"container.*"}""",
        ).evaluate(
            DataContext(hashMapOf()).withSlotSourceInventoryAccess { _, _ -> selected },
        ) shouldContainExactly selected
    }

    test("slot_range defaults its source and accepts multiple slot patterns") {
        val selected = listOf(item(Material.STONE))
        slotSource("""{"type":"minecraft:slot_range","slots":["hotbar.*","armor.*"]}""")
            .evaluate(DataContext(hashMapOf()).withSlotSourceInventoryAccess { source, patterns ->
                source shouldBe "container"
                patterns shouldBe listOf("hotbar.*", "armor.*")
                selected
            }) shouldContainExactly selected
    }

    test("group concatenates sources in order and preserves repeated selections") {
        val stone = item(Material.STONE)
        val diamond = item(Material.DIAMOND)
        val result = slotSource(
            """{"type":"minecraft:group","sources":[{"type":"minecraft:slot_range","source":"this","slots":"hotbar.*"},{"type":"minecraft:slot_range","source":"this","slots":"hotbar.0"}]}""",
        ).evaluate(
            DataContext(hashMapOf()).withSlotSourceInventoryAccess { _, pattern ->
                when (pattern) {
                    listOf("hotbar.*") -> listOf(stone, diamond)
                    listOf("hotbar.0") -> listOf(stone)
                    else -> error("Unexpected slot pattern: $pattern")
                }
            },
        )

        result shouldContainExactly listOf(stone, diamond, stone)
    }

    test("filtered retains only items accepted by the item predicate matcher") {
        val stone = item(Material.STONE)
        val diamond = item(Material.DIAMOND)
        val result = slotSource(
            """{"type":"minecraft:filtered","item_filter":{"items":"minecraft:diamond"},"slot_source":{"type":"minecraft:slot_range","source":"this","slots":"container.*"}}""",
        ).evaluate(
            DataContext(hashMapOf())
                .withSlotSourceInventoryAccess { _, _ -> listOf(stone, diamond) }
                .withItemPredicateMatcher { candidate, _ -> candidate.material() == Material.DIAMOND },
        )

        result shouldContainExactly listOf(diamond)
    }

    test("limit_slots keeps the first selected items up to the requested limit") {
        val selected = listOf(item(Material.STONE), item(Material.DIAMOND), item(Material.GOLD_INGOT))
        val result = slotSource(
            """{"type":"minecraft:limit_slots","limit":2,"slot_source":{"type":"minecraft:slot_range","source":"this","slots":"container.*"}}""",
        ).evaluate(DataContext(hashMapOf()).withSlotSourceInventoryAccess { _, _ -> selected })

        result shouldContainExactly selected.take(2)
    }

    test("limit_slots counts empty slots") {
        val selected = listOf(item(Material.STONE), ItemStack.AIR, item(Material.DIAMOND))
        slotSource(
            """{"type":"minecraft:limit_slots","limit":2,"slot_source":{"type":"minecraft:slot_range","source":"this","slots":"container.*"}}""",
        ).evaluate(DataContext(hashMapOf()).withSlotSourceInventoryAccess { _, _ -> selected }) shouldContainExactly selected.take(2)
    }

    envTest("contents reads the selected item's inventory component") { _ ->
        val nestedItem = item(Material.DIAMOND)
        val container = item(Material.SHULKER_BOX).with(DataComponents.CONTAINER, listOf(nestedItem))
        val result = slotSource(
            """{"type":"minecraft:contents","component":"minecraft:container","slot_source":{"type":"minecraft:slot_range","source":"this","slots":"weapon.mainhand"}}""",
        ).evaluate(DataContext(hashMapOf()).withSlotSourceInventoryAccess { _, _ -> listOf(container) })

        result shouldContainExactly listOf(nestedItem)
    }
}
