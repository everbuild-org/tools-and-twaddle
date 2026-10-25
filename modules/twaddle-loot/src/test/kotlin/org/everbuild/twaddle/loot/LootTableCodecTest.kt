package org.everbuild.twaddle.loot

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.loot.entry.createVanillaLootEntryTypeRegistry
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

private object LootTableCodecTestAnchor

private fun decodeTable(json: String): LootTable = when (
    val result = lootTableCodec.decode(Transcoder.JSON, JsonParser.parseString(json))
) {
    is Result.Ok -> result.value
    is Result.Error -> error("Could not decode loot table: ${result.message}")
}

private fun lootTableFixtureDirectory(): Path = Path.of(
    requireNotNull(LootTableCodecTestAnchor::class.java.classLoader.getResource("data/loot_test/loot_table")) {
        "Loot-table fixtures are missing from test resources"
    }.toURI(),
)

val lootTableCodecTests by testSuite("Loot table codecs") {
    test("table defaults and singular or list modifier and condition fields decode") {
        val fixture = Files.readString(lootTableFixtureDirectory().resolve("coverage/table_codec_fields.json"))
        val table = decodeTable(fixture)

        table.type shouldBe "generic"
        table.modifiers.size shouldBe 2
        table.pools.single().predicates.size shouldBe 1
        table.pools.single().modifiers.size shouldBe 1
        table.pools.single().entries.single().predicates.size shouldBe 1
        table.pools.single().entries.single().modifiers.size shouldBe 1
    }

    test("all representative loot-table fixtures decode and round-trip") {
        val fixtures = Files.walk(lootTableFixtureDirectory()).use { paths ->
            paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".json") }
                .sorted()
                .toList()
        }
        fixtures.shouldNotBeEmpty()

        fixtures.forEach { fixture ->
            try {
                val table = decodeTable(Files.readString(fixture))
                val json = when (val encoded = lootTableCodec.encode(Transcoder.JSON, table)) {
                    is Result.Ok -> encoded.value
                    is Result.Error -> error(encoded.message)
                }
                decodeTable(json.toString())
            } catch (failure: Throwable) {
                throw AssertionError("Could not decode loot-table fixture ${fixture.fileName}", failure)
            }
        }
    }

    test("an empty table decodes to generic with no pools") {
        val table = decodeTable("{}")

        table.type shouldBe "generic"
        table.pools shouldBe emptyList()
    }

    test("loot table entries decode and encode both references and inline tables") {
        val table = decodeTable(
            """{"pools":[{"rolls":1,"entries":[{"type":"minecraft:loot_table","value":{"pools":[{"rolls":1,"entries":[{"type":"minecraft:item","name":"minecraft:diamond"}]}]}}]}]}""",
        )

        val encoded = when (val result = lootTableCodec.encode(Transcoder.JSON, table)) {
            is Result.Ok -> result.value
            is Result.Error -> error("Could not encode inline loot table: ${result.message}")
        }
        val decodedAgain = decodeTable(encoded.toString())
        decodedAgain shouldBe table
    }
}
