package org.everbuild.twaddle.datapack_primitives.number_provider

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.floats.shouldBeNaN
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

private val floatProviderCodec = DatapackPrimitives.vanilla().floatNumberProviderCodec

private fun floatProvider(json: String): NumberProviderDeclaration<Float> =
    when (val decoded = floatProviderCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode float provider: ${decoded.message}")
    }

private fun evaluate(json: String): Float = floatProvider(json).get(DataContext(hashMapOf()), Random(42))

val floatNumberProviderEvaluationTests by testSuite("Float number provider evaluation") {
    test("abs returns the non-negative magnitude") {
        evaluate("""{"type":"minecraft:abs","input":-2.5}""") shouldBe 2.5f
    }

    test("add and avg combine all inputs, with their empty identities") {
        evaluate("""{"type":"minecraft:add","inputs":[1.5,2.5]}""") shouldBe 4f
        evaluate("""{"type":"minecraft:add","inputs":[]}""") shouldBe 0f
        evaluate("""{"type":"minecraft:avg","inputs":[1.0,3.0,5.0]}""") shouldBe 3f
        evaluate("""{"type":"minecraft:avg","inputs":[]}""") shouldBe 0f
    }

    test("division and integer conversion preserve float values") {
        evaluate("""{"type":"minecraft:div","left":7.0,"right":2.0}""") shouldBe 3.5f
        evaluate("""{"type":"minecraft:from_int","input":-3}""") shouldBe -3f
    }

    test("max and min select the largest and smallest inputs") {
        evaluate("""{"type":"minecraft:max","inputs":[1.0,-2.0,5.0]}""") shouldBe 5f
        evaluate("""{"type":"minecraft:min","inputs":[1.0,-2.0,5.0]}""") shouldBe -2f
    }

    test("max and min reject empty input lists") {
        shouldThrow<IllegalArgumentException> {
            evaluate("""{"type":"minecraft:max","inputs":[]}""")
        }
        shouldThrow<IllegalArgumentException> {
            evaluate("""{"type":"minecraft:min","inputs":[]}""")
        }
    }

    test("mod follows floating point remainder semantics for negative dividends") {
        evaluate("""{"type":"minecraft:mod","left":-7.5,"right":2.0}""") shouldBe -1.5f
    }

    test("mul and negate evaluate recursively") {
        evaluate("""{"type":"minecraft:mul","inputs":[2.0,{"type":"minecraft:abs","input":-3.0}]}""") shouldBe 6f
        evaluate("""{"type":"minecraft:negate","input":2.5}""") shouldBe -2.5f
    }

    test("pow raises the base to the exponent") {
        evaluate("""{"type":"minecraft:pow","base":2.0,"exponent":3.0}""") shouldBe 8f
    }

    test("sin and cos use radians") {
        evaluate("""{"type":"minecraft:sin","input":1.5707964}""") shouldBe 1f
        evaluate("""{"type":"minecraft:cos","input":3.1415927}""") shouldBe -1f
    }

    test("sqrt returns the root and propagates NaN for negative inputs") {
        evaluate("""{"type":"minecraft:sqrt","input":9.0}""") shouldBe 3f
        evaluate("""{"type":"minecraft:sqrt","input":-1.0}""").shouldBeNaN()
    }

    test("floor and ceil round toward negative and positive infinity") {
        evaluate("""{"type":"minecraft:floor","input":-2.1}""") shouldBe -3f
        evaluate("""{"type":"minecraft:ceil","input":-2.1}""") shouldBe -2f
    }

    test("round uses half-up behavior, including negative halfway values") {
        evaluate("""{"type":"minecraft:round","input":2.5}""") shouldBe 3f
        evaluate("""{"type":"minecraft:round","input":-2.5}""") shouldBe -2f
    }

    test("truncate removes the fractional part toward zero") {
        evaluate("""{"type":"minecraft:truncate","input":-2.9}""") shouldBe -2f
    }

    test("length computes the Euclidean length of its inputs") {
        evaluate("""{"type":"minecraft:length","inputs":[3.0,4.0]}""") shouldBe 5f
        evaluate("""{"type":"minecraft:length","inputs":[]}""") shouldBe 0f
    }
}
