package org.everbuild.twaddle.datapack_primitives.predicate

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import net.kyori.adventure.key.Key
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.withEnchantment
import java.util.Random

private val predicateCodec = DatapackPrimitives.vanilla().contextPredicateCodec

private fun predicate(json: String): ContextPredicate =
    when (val decoded = predicateCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode context predicate: ${decoded.message}")
    }

private fun enchantments(level: Int) = EnchantmentList(
    linkedMapOf(RegistryKey.of<Enchantment>(Key.key("minecraft:fortune")) to level),
)

val predicateCompositionEvaluationTests by testSuite("Context predicate composition evaluation") {
    val context = DataContext(hashMapOf())

    test("all_of requires every term and accepts an empty list") {
        predicate("""{"type":"minecraft:all_of","terms":[]}""").test(context, Random(0)) shouldBe true
        predicate("""{"type":"minecraft:all_of","terms":[{"type":"minecraft:random_chance","chance":1.0},{"type":"minecraft:random_chance","chance":0.0}]}""")
            .test(context, Random(0)) shouldBe false
    }

    test("any_of accepts one matching term and rejects an empty list") {
        predicate("""{"type":"minecraft:any_of","terms":[]}""").test(context, Random(0)) shouldBe false
        predicate("""{"type":"minecraft:any_of","terms":[{"type":"minecraft:random_chance","chance":0.0},{"type":"minecraft:random_chance","chance":1.0}]}""")
            .test(context, Random(0)) shouldBe true
    }

    test("inverted negates its nested predicate") {
        predicate("""{"type":"minecraft:inverted","term":{"type":"minecraft:random_chance","chance":0.0}}""")
            .test(context, Random(0)) shouldBe true
        predicate("""{"type":"minecraft:inverted","term":{"type":"minecraft:random_chance","chance":1.0}}""")
            .test(context, Random(0)) shouldBe false
    }

    test("compositions short-circuit after a decisive term") {
        val allOf = predicate(
            """{"type":"minecraft:all_of","terms":[{"type":"minecraft:random_chance","chance":0.0},{"type":"minecraft:random_chance","chance":1.0}]}""",
        )
        val anyOf = predicate(
            """{"type":"minecraft:any_of","terms":[{"type":"minecraft:random_chance","chance":1.0},{"type":"minecraft:random_chance","chance":0.0}]}""",
        )

        val expectedNextFloat = Random(23).apply { nextFloat() }.nextFloat()
        val allRandom = Random(23)
        allOf.test(context, allRandom) shouldBe false
        allRandom.nextFloat() shouldBe expectedNextFloat

        val anyRandom = Random(23)
        anyOf.test(context, anyRandom) shouldBe true
        anyRandom.nextFloat() shouldBe expectedNextFloat
    }

    test("random_chance follows the supplied seeded random source") {
        val chance = predicate("""{"type":"minecraft:random_chance","chance":0.5}""")
        for (seed in listOf(0L, 1L, 42L, 1234L)) {
            chance.test(context, Random(seed)) shouldBe (Random(seed).nextFloat() < 0.5f)
        }
    }

    test("random_chance_with_enchanted_bonus uses the unenchanted and scalar enchanted chances") {
        val chance = predicate(
            """{"type":"minecraft:random_chance_with_enchanted_bonus","enchantment":"minecraft:fortune","unenchanted_chance":0.25,"enchanted_chance":0.9}""",
        )
        val otherEnchantment = EnchantmentList(
            linkedMapOf(RegistryKey.of<Enchantment>(Key.key("minecraft:looting")) to 3),
        )

        // Random(0)'s first float is about 0.73: this distinguishes 0.25 from 0.9.
        chance.test(DataContext(hashMapOf()).withEnchantment(null), Random(0)) shouldBe false
        chance.test(DataContext(hashMapOf()).withEnchantment(enchantments(1)), Random(0)) shouldBe true
        chance.test(DataContext(hashMapOf()).withEnchantment(otherEnchantment), Random(0)) shouldBe false
    }

    test("random_chance_with_enchanted_bonus increases the linear chance above level one") {
        val chance = predicate(
            """{"type":"minecraft:random_chance_with_enchanted_bonus","enchantment":"minecraft:fortune","unenchanted_chance":0.99,"enchanted_chance":{"type":"minecraft:linear","base":0.5,"per_level_above_first":0.25}}""",
        )

        // Random(0)'s first float is about 0.73: level one fails at 0.5, level two passes at 0.75.
        chance.test(DataContext(hashMapOf()).withEnchantment(enchantments(1)), Random(0)) shouldBe false
        chance.test(DataContext(hashMapOf()).withEnchantment(enchantments(2)), Random(0)) shouldBe true
    }

    test("table_bonus uses level zero when absent and caps high levels at the final chance") {
        val bonus = predicate(
            """{"type":"minecraft:table_bonus","enchantment":"minecraft:fortune","chances":[0.0,0.0,1.0]}""",
        )

        bonus.test(DataContext(hashMapOf()).withEnchantment(null), Random(0)) shouldBe false
        bonus.test(DataContext(hashMapOf()).withEnchantment(enchantments(2)), Random(0)) shouldBe true
        bonus.test(DataContext(hashMapOf()).withEnchantment(enchantments(20)), Random(0)) shouldBe true
    }
}
