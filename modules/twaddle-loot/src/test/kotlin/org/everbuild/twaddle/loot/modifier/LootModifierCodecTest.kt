package org.everbuild.twaddle.loot.modifier

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result.Ok
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import java.util.Random
import java.nio.file.Files
import java.nio.file.Path
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.impl.SetCountLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetPotionLootModifier

private data class TestPredicate(val raw: Codec.RawValue) : ContextPredicate {
    override fun type() = Key.key("loot_test:predicate")
    override fun test(context: DataContext, random: Random) = false
}
private data class TestIntNumberProvider(val raw: Codec.RawValue) : NumberProviderDeclaration<Int> {
    override fun type() = Key.key("loot_test:integer")
    override fun get(context: DataContext, random: Random): Int = error("Test provider is decode-only")
}
private data class TestFloatNumberProvider(val raw: Codec.RawValue) : NumberProviderDeclaration<Float> {
    override fun type() = Key.key("loot_test:float")
    override fun get(context: DataContext, random: Random): Float = error("Test provider is decode-only")
}

private val predicateCodec: Codec<ContextPredicate> = Codec.RAW_VALUE.transform<ContextPredicate>(
    ::TestPredicate,
    { (it as TestPredicate).raw },
)
private val intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>> = Codec.RAW_VALUE.transform<NumberProviderDeclaration<Int>>(
    ::TestIntNumberProvider,
    { (it as TestIntNumberProvider).raw },
)
private val floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>> = Codec.RAW_VALUE.transform<NumberProviderDeclaration<Float>>(
    ::TestFloatNumberProvider,
    { (it as TestFloatNumberProvider).raw },
)
private val modifierCodec = createLootModifierCodec(
    createVanillaLootModifierTypeRegistry(),
    predicateCodec,
    intNumberProviderCodec,
    floatNumberProviderCodec,
)

private fun decodeModifier(json: String): LootModifier =
    when (val decoded = modifierCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode loot modifier: ${decoded.message}")
    }

val lootModifierCodecTests by testSuite("Loot modifier codecs") {
    test("set_count decodes its registered type and defaults add to false") {
        val modifier = decodeModifier("""{"type":"minecraft:set_count","count":"fixed"}""")

        modifier shouldBe SetCountLootModifier(TestIntNumberProvider(decodeRaw("\"fixed\"")), add = false)
    }

    test("set_count preserves an explicit add flag") {
        val modifier = decodeModifier("""{"type":"minecraft:set_count","count":"fixed","add":true}""")

        modifier shouldBe SetCountLootModifier(TestIntNumberProvider(decodeRaw("\"fixed\"")), add = true)
    }

    test("set_count encodes its registered type and omits the default add flag") {
        val modifier = SetCountLootModifier(TestIntNumberProvider(decodeRaw("\"fixed\"")))
        val encoded = modifierCodec.encode(Transcoder.JSON, modifier)

        (encoded as Ok).value shouldBe JsonParser.parseString(
            """{"type":"minecraft:set_count","count":"fixed"}""",
        )
    }

    test("set_potion decodes its registry key") {
        val modifier = decodeModifier("""{"type":"minecraft:set_potion","id":"minecraft:healing"}""")

        modifier shouldBe SetPotionLootModifier(Key.key("minecraft:healing"))
    }

    test("all modifier fixtures decode through the registered union") {
        val fixtureDirectory = Path.of(
            requireNotNull(javaClass.classLoader.getResource("data/loot_test/loot_table/coverage/modifiers")) {
                "Modifier fixture directory is missing from the test resources"
            }.toURI(),
        )
        val fixtures = Files.list(fixtureDirectory).use { files -> files.filter { it.toString().endsWith(".json") }.toList() }

        fixtures.size shouldBe 20
        fixtures.forEach { fixture ->
            val lootTable = JsonParser.parseString(Files.readString(fixture)).asJsonObject
            val modifier = lootTable["pools"].asJsonArray[0].asJsonObject["entries"].asJsonArray[0]
                .asJsonObject["modifier"].asJsonObject

            try {
                val decoded = decodeModifier(modifier.toString())
                val encoded = modifierCodec.encode(Transcoder.JSON, decoded)
                if (encoded !is Ok) error("Could not encode decoded modifier")
                decodeModifier(encoded.value.toString()).type() shouldBe decoded.type()
            } catch (failure: Throwable) {
                throw AssertionError("Could not decode modifier fixture ${fixture.fileName}", failure)
            }
        }
    }

}

private fun decodeRaw(json: String): Codec.RawValue = when (
    val decoded = Codec.RAW_VALUE.decode(Transcoder.JSON, JsonParser.parseString(json))
) {
    is Result.Ok -> decoded.value
    is Result.Error -> error("Could not decode raw value: ${decoded.message}")
}
