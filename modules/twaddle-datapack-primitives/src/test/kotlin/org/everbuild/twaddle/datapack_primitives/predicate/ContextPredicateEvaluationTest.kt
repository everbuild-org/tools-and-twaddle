package org.everbuild.twaddle.datapack_primitives.predicate

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.attribute.EnvironmentAttributeMap
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.LocationContext
import org.everbuild.twaddle.datapack_primitives.context.LocationRange
import org.everbuild.twaddle.datapack_primitives.context.WeatherState
import org.everbuild.twaddle.datapack_primitives.context.withEnchantmentActive
import org.everbuild.twaddle.datapack_primitives.context.withEnvironment
import org.everbuild.twaddle.datapack_primitives.context.withExplosionRadius
import org.everbuild.twaddle.datapack_primitives.context.withLocation
import org.everbuild.twaddle.datapack_primitives.context.withWeather
import org.everbuild.twaddle.datapack_primitives.context.withWorldTime
import java.util.Random

private val contextPredicateCodec = DatapackPrimitives.vanilla().contextPredicateCodec

private fun predicate(json: String): ContextPredicate =
    when (val decoded = contextPredicateCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode context predicate: ${decoded.message}")
    }

val contextPredicateEvaluationTests by testSuite("Context predicate evaluation") {
    test("integer value checks support exact values and inclusive ranges") {
        val exact = predicate(
            """{"type":"minecraft:int_value_check","value":7,"test":{"type":"minecraft:constant","value":7}}""",
        )
        val range = predicate(
            """{"type":"minecraft:int_value_check","value":7,"test":{"min":7,"max":9}}""",
        )
        val outsideRange = predicate(
            """{"type":"minecraft:int_value_check","value":10,"test":{"min":7,"max":9}}""",
        )
        val context = DataContext(hashMapOf())

        exact.test(context, Random(0)) shouldBe true
        predicate("""{"type":"minecraft:int_value_check","value":8,"test":7}""").test(context, Random(0)) shouldBe false
        range.test(context, Random(0)) shouldBe true
        outsideRange.test(context, Random(0)) shouldBe false
    }

    test("float value checks support exact values and inclusive ranges") {
        val exact = predicate(
            """{"type":"minecraft:float_value_check","value":2.5,"test":2.5}""",
        )
        val range = predicate(
            """{"type":"minecraft:float_value_check","value":3.5,"test":{"min":2.5,"max":3.5}}""",
        )
        val outsideRange = predicate(
            """{"type":"minecraft:float_value_check","value":3.51,"test":{"min":2.5,"max":3.5}}""",
        )
        val context = DataContext(hashMapOf())

        exact.test(context, Random(0)) shouldBe true
        range.test(context, Random(0)) shouldBe true
        outsideRange.test(context, Random(0)) shouldBe false
    }

    test("time checks use the requested clock and compare periodic time") {
        val timeCheck = predicate(
            """{"type":"minecraft:time_check","clock":"minecraft:overworld","value":{"min":1000,"max":1000},"period":24000}""",
        )
        val requestedClocks = mutableListOf<Key>()
        val context = DataContext(hashMapOf()).withWorldTime { clock ->
            requestedClocks += clock
            25_000L
        }

        timeCheck.test(context, Random(0)) shouldBe true
        requestedClocks shouldBe listOf(Key.key("minecraft:overworld"))
        shouldThrow<IllegalStateException> { timeCheck.test(DataContext(hashMapOf()), Random(0)) }
    }

    test("weather checks distinguish rain from thunder and allow omitted fields") {
        val both = predicate(
            """{"type":"minecraft:weather_check","raining":true,"thundering":true}""",
        )
        val noThunder = predicate(
            """{"type":"minecraft:weather_check","raining":true,"thundering":false}""",
        )
        val anyWeather = predicate("""{"type":"minecraft:weather_check"}""")

        both.test(DataContext(hashMapOf()).withWeather(WeatherState(raining = false, thundering = true)), Random(0)) shouldBe true
        noThunder.test(DataContext(hashMapOf()).withWeather(WeatherState(raining = true, thundering = false)), Random(0)) shouldBe true
        both.test(DataContext(hashMapOf()).withWeather(WeatherState(raining = false, thundering = false)), Random(0)) shouldBe false
        anyWeather.test(DataContext(hashMapOf()).withWeather(WeatherState(raining = false, thundering = false)), Random(0)) shouldBe true
        shouldThrow<IllegalStateException> { anyWeather.test(DataContext(hashMapOf()), Random(0)) }
    }

    test("location checks pass the offset position and decoded predicate to the evaluator") {
        val locationCheck = predicate(
            """{"type":"minecraft:location_check","offsetX":1,"offsetY":-2,"offsetZ":3,"predicate":{"position":{"x":11.0,"y":18.0,"z":33.0}}}""",
        )
        val context = DataContext(hashMapOf()).withLocation(
            LocationContext(
                origin = ContextLocation(10.0, 20.0, 30.0),
                evaluator = { position, decodedPredicate ->
                    position shouldBe ContextLocation(11.0, 18.0, 33.0)
                    decodedPredicate.position?.x shouldBe LocationRange(11.0, 11.0)
                    decodedPredicate.position?.y shouldBe LocationRange(18.0, 18.0)
                    decodedPredicate.position?.z shouldBe LocationRange(33.0, 33.0)
                    true
                },
            ),
        )

        locationCheck.test(context, Random(0)) shouldBe true
        shouldThrow<IllegalStateException> { locationCheck.test(DataContext(hashMapOf()), Random(0)) }
    }

    test("environment attribute checks compare the supplied attribute value") {
        val attribute = EnvironmentAttribute.PIGLINS_ZOMBIFY
        val check = predicate(
            """{"type":"minecraft:environment_attribute_check","attribute":"minecraft:gameplay/piglins_zombify","value":false}""",
        )
        val expectedByDefault = predicate(
            """{"type":"minecraft:environment_attribute_check","attribute":"minecraft:gameplay/piglins_zombify","value":true}""",
        )

        check.test(
            DataContext(hashMapOf()).withEnvironment(EnvironmentAttributeMap.builder().set(attribute, false).build()),
            Random(0),
        ) shouldBe true
        check.test(DataContext(hashMapOf()).withEnvironment(EnvironmentAttributeMap.EMPTY), Random(0)) shouldBe false
        expectedByDefault.test(DataContext(hashMapOf()).withEnvironment(EnvironmentAttributeMap.EMPTY), Random(0)) shouldBe true
        shouldThrow<IllegalStateException> { check.test(DataContext(hashMapOf()), Random(0)) }
    }

    test("survives explosion uses the radius and supplied random source") {
        val survives = predicate("""{"type":"minecraft:survives_explosion"}""")

        survives.test(DataContext(hashMapOf()).withExplosionRadius(null), Random(0)) shouldBe true
        survives.test(DataContext(hashMapOf()).withExplosionRadius(0f), Random(0)) shouldBe true
        for (seed in listOf(0L, 1L, 42L, 123L)) {
            val expected = Random(seed).nextFloat() < 0.5f
            survives.test(DataContext(hashMapOf()).withExplosionRadius(2f), Random(seed)) shouldBe expected
        }
        shouldThrow<IllegalStateException> { survives.test(DataContext(hashMapOf()), Random(0)) }
    }

    test("enchantment active checks match the nullable context state") {
        val active = predicate("""{"type":"minecraft:enchantment_active_check","active":true}""")
        val inactive = predicate("""{"type":"minecraft:enchantment_active_check","active":false}""")

        active.test(DataContext(hashMapOf()).withEnchantmentActive(true), Random(0)) shouldBe true
        active.test(DataContext(hashMapOf()).withEnchantmentActive(false), Random(0)) shouldBe false
        inactive.test(DataContext(hashMapOf()).withEnchantmentActive(null), Random(0)) shouldBe false
        shouldThrow<IllegalStateException> { active.test(DataContext(hashMapOf()), Random(0)) }
    }
}
