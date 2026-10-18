@file:Suppress("SameParameterValue")

package org.everbuild.twaddle.datapack_primitives.number_provider

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.StorageAccessor
import org.everbuild.twaddle.datapack_primitives.context.withStorage
import java.util.Random

private val randomProviderPrimitives = DatapackPrimitives.vanilla()

private fun intProvider(json: String): NumberProviderDeclaration<Int> =
    when (val decoded = randomProviderPrimitives.intNumberProviderCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode integer provider: ${decoded.message}")
    }

private fun floatProvider(json: String): NumberProviderDeclaration<Float> =
    when (val decoded = randomProviderPrimitives.floatNumberProviderCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode float provider: ${decoded.message}")
    }

val randomAndDispatchNumberProviderEvaluationTests by testSuite("Random and dispatch number provider evaluation") {
    test("uniform integer includes both endpoints and follows the supplied seed") {
        val seed = 712L
        val expectedRandom = Random(seed)
        val expected = expectedRandom.nextLong(2L, 8L).toInt()

        intProvider("""{"type":"minecraft:uniform","min":2,"max":7}""")
            .get(DataContext(hashMapOf()), Random(seed)) shouldBe expected
    }

    test("uniform float interpolates using the supplied seeded random value") {
        val seed = 41L
        val randomValue = Random(seed).nextFloat()
        val expected = -2.0f + randomValue * 5.0f

        floatProvider("""{"type":"minecraft:uniform","min":-2.0,"max":3.0}""")
            .get(DataContext(hashMapOf()), Random(seed)) shouldBe expected
    }

    test("binomial performs one seeded trial per non-negative integer count") {
        val seed = 31L
        val expectedRandom = Random(seed)
        val expected = (0 until 6).count { expectedRandom.nextFloat() < 0.4f }

        intProvider("""{"type":"minecraft:binomial","n":6,"p":0.4}""")
            .get(DataContext(hashMapOf()), Random(seed)) shouldBe expected
    }

    test("weighted_list uses weights for selection and evaluates the selected nested provider with the same random") {
        val seed = 87L
        val expectedRandom = Random(seed)
        val selected = expectedRandom.nextLong(3L)
        val expected = if (selected < 1L) {
            10 + expectedRandom.nextLong(2L, 5L).toInt()
        } else {
            20
        }
        val provider = intProvider(
            """{"type":"minecraft:weighted_list","distribution":[{"data":{"type":"minecraft:uniform","min":10,"max":13},"weight":1},{"data":20,"weight":2}]}""",
        )

        provider.get(DataContext(hashMapOf()), Random(seed)) shouldBe expected
    }

    test("conditional selects the matching branch and passes the original context to its nested provider") {
        val context = DataContext(hashMapOf()).withStorage(
            object : StorageAccessor<Int> {
                override fun getValue(value: String): Int = 23
            },
        )
        val provider = intProvider(
            """{"type":"minecraft:conditional","condition":{"type":"minecraft:int_value_check","value":7,"test":7},"on_true":{"type":"minecraft:storage","storage":"minecraft:test","path":"value"},"on_false":-1}""",
        )

        provider.get(context, Random(2L)) shouldBe 23
    }

    test("conditional selects the false branch when its condition does not match") {
        val provider = intProvider(
            """{"type":"minecraft:conditional","condition":{"type":"minecraft:int_value_check","value":7,"test":8},"on_true":1,"on_false":-2}""",
        )

        provider.get(DataContext(hashMapOf()), Random(2L)) shouldBe -2
    }

    test("number_dispatcher selects the first matching case and evaluates its value with the same context") {
        val context = DataContext(hashMapOf()).withStorage(
            object : StorageAccessor<Int> {
                override fun getValue(value: String): Int = when (value) {
                    "first" -> 51
                    "second" -> 62
                    else -> error("Unexpected storage path: $value")
                }
            },
        )
        val provider = intProvider(
            """{"type":"minecraft:number_dispatcher","cases":[{"condition":{"type":"minecraft:int_value_check","value":1,"test":1},"value":{"type":"minecraft:storage","storage":"minecraft:test","path":"first"}},{"condition":{"type":"minecraft:int_value_check","value":1,"test":1},"value":{"type":"minecraft:storage","storage":"minecraft:test","path":"second"}}],"default":-1}""",
        )

        provider.get(context, Random(3L)) shouldBe 51
    }

    test("number_dispatcher evaluates the default when no case matches") {
        val provider = intProvider(
            """{"type":"minecraft:number_dispatcher","cases":[{"condition":{"type":"minecraft:int_value_check","value":1,"test":2},"value":10}],"default":-1}""",
        )

        provider.get(DataContext(hashMapOf()), Random(4L)) shouldBe -1
    }
}
