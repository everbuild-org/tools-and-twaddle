package org.everbuild.twaddle.loot.modifier

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result.Ok
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.loot.condition.LootCondition
import org.everbuild.twaddle.loot.modifier.impl.SetCountLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetPotionLootModifier
import org.everbuild.twaddle.loot.number_provider.LootNumberProvider

private data class TestCondition(val id: String) : LootCondition
private data class TestNumberProvider(val id: String) : LootNumberProvider

private val conditionCodec: Codec<LootCondition> = Codec.STRING.transform<LootCondition>(
    ::TestCondition,
    { (it as TestCondition).id },
)
private val numberProviderCodec: Codec<LootNumberProvider> = Codec.STRING.transform<LootNumberProvider>(
    ::TestNumberProvider,
    { (it as TestNumberProvider).id },
)
private val modifierCodec = createLootModifierCodec(
    createVanillaLootModifierTypeRegistry(),
    conditionCodec,
    numberProviderCodec,
    numberProviderCodec,
)

private fun decodeModifier(json: String): LootModifier =
    when (val decoded = modifierCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode loot modifier: ${decoded.message}")
    }

val lootModifierCodecTests by testSuite("Loot modifier codecs") {
    test("set_count decodes its registered type and defaults add to false") {
        val modifier = decodeModifier("""{"type":"minecraft:set_count","count":"fixed"}""")

        modifier shouldBe SetCountLootModifier(TestNumberProvider("fixed"), add = false)
    }

    test("set_count preserves an explicit add flag") {
        val modifier = decodeModifier("""{"type":"minecraft:set_count","count":"fixed","add":true}""")

        modifier shouldBe SetCountLootModifier(TestNumberProvider("fixed"), add = true)
    }

    test("set_count encodes its registered type and omits the default add flag") {
        val modifier = SetCountLootModifier(TestNumberProvider("fixed"))
        val encoded = modifierCodec.encode(Transcoder.JSON, modifier)

        (encoded as Ok).value shouldBe JsonParser.parseString(
            """{"type":"minecraft:set_count","count":"fixed"}""",
        )
    }

    test("set_potion decodes its registry key") {
        val modifier = decodeModifier("""{"type":"minecraft:set_potion","id":"minecraft:healing"}""")

        modifier shouldBe SetPotionLootModifier(Key.key("minecraft:healing"))
    }
}
