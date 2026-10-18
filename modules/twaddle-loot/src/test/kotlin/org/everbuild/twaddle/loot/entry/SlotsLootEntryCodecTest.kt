package org.everbuild.twaddle.loot.entry

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.withSlotSourceInventoryAccess
import org.everbuild.twaddle.loot.LootTable
import org.everbuild.twaddle.loot.createLootTableCodec
import org.everbuild.twaddle.loot.entry.impl.SlotsLootEntryData
import org.everbuild.twaddle.loot.modifier.createVanillaLootModifierTypeRegistry
import java.nio.file.Files
import java.nio.file.Path

private val primitives = DatapackPrimitives.vanilla()
private val lootTableCodec = createLootTableCodec(
    primitives.contextPredicateCodec,
    createVanillaLootEntryTypeRegistry(),
    createVanillaLootModifierTypeRegistry(),
    primitives.intNumberProviderCodec,
    primitives.floatNumberProviderCodec,
    primitives.slotSourceCodec,
)

private fun decodeTable(json: String): LootTable = when (
    val result = lootTableCodec.decode(Transcoder.JSON, JsonParser.parseString(json))
) {
    is Result.Ok -> result.value
    is Result.Error -> error("Could not decode loot table: ${result.message}")
}

val slotsLootEntryCodecTests by testSuite("Slots loot entry") {
    test("decodes a slots entry with array shorthand and evaluates its slot source") {
        val fixturePath = Path.of(
            requireNotNull(javaClass.classLoader.getResource("data/loot_test/loot_table/coverage/entries/slots.json")) {
                "Slots loot-entry fixture is missing from test resources"
            }.toURI(),
        )
        val table = decodeTable(Files.readString(fixturePath))
        val entry = table.pools.single().entries.single()
        val data = entry.value as SlotsLootEntryData
        entry.value.type().asString() shouldBe "minecraft:slots"

        val stone = ItemStack.of(Material.STONE)
        val diamond = ItemStack.of(Material.DIAMOND)
        val selected = data.slotSource.evaluate(
            DataContext(hashMapOf()).withSlotSourceInventoryAccess { source, patterns ->
                source shouldBe "this"
                when (patterns) {
                    listOf("hotbar.*") -> listOf(stone)
                    listOf("armor.*") -> listOf(diamond)
                    else -> error("Unexpected slot patterns: $patterns")
                }
            },
        )

        selected shouldContainExactly listOf(stone, diamond)
    }
}
