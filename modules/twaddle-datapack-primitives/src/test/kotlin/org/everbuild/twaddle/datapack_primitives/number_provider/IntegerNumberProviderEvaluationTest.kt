package org.everbuild.twaddle.datapack_primitives.number_provider

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

val integerNumberProviderEvaluationTests by testSuite("Integer number provider evaluation") {
    val primitives = DatapackPrimitives.vanilla()

    test("abs evaluates its input and follows integer overflow behavior") {
        evaluate(primitives, """{"type":"minecraft:abs","input":-5}""") shouldBe 5
        evaluate(primitives, """{"type":"minecraft:abs","input":-2147483648}""") shouldBe Int.MIN_VALUE
    }

    test("add sums all inputs with integer overflow") {
        evaluate(primitives, """{"type":"minecraft:add","inputs":[-2,5,3]}""") shouldBe 6
        evaluate(primitives, """{"type":"minecraft:add","inputs":[2147483647,1]}""") shouldBe Int.MIN_VALUE
    }

    test("avg divides the sum by the number of inputs using integer division") {
        evaluate(primitives, """{"type":"minecraft:avg","inputs":[-2,1]}""") shouldBe 0
        evaluate(primitives, """{"type":"minecraft:avg","inputs":[]}""") shouldBe 0
    }

    test("div truncates toward zero") {
        evaluate(primitives, """{"type":"minecraft:div","left":-7,"right":2}""") shouldBe -3
    }

    test("floor_div rounds negative non-integral quotients down") {
        evaluate(primitives, """{"type":"minecraft:floor_div","left":-7,"right":2}""") shouldBe -4
        evaluate(primitives, """{"type":"minecraft:floor_div","left":7,"right":-2}""") shouldBe -4
    }

    test("floor_mod follows the divisor sign for negative operands") {
        evaluate(primitives, """{"type":"minecraft:floor_mod","left":-8,"right":3}""") shouldBe 1
        evaluate(primitives, """{"type":"minecraft:floor_mod","left":8,"right":-3}""") shouldBe -1
    }

    test("from_float truncates toward zero") {
        evaluate(primitives, """{"type":"minecraft:from_float","input":2.75}""") shouldBe 2
        evaluate(primitives, """{"type":"minecraft:from_float","input":-2.75}""") shouldBe -2
    }

    test("max and min select the corresponding input across negative values") {
        evaluate(primitives, """{"type":"minecraft:max","inputs":[-8,-2,-5]}""") shouldBe -2
        evaluate(primitives, """{"type":"minecraft:min","inputs":[-8,-2,-5]}""") shouldBe -8
    }

    test("mod keeps the dividend sign, unlike floor_mod") {
        evaluate(primitives, """{"type":"minecraft:mod","left":-8,"right":3}""") shouldBe -2
    }

    test("mul multiplies all inputs with integer overflow") {
        evaluate(primitives, """{"type":"minecraft:mul","inputs":[-2,3,4]}""") shouldBe -24
        evaluate(primitives, """{"type":"minecraft:mul","inputs":[2147483647,2]}""") shouldBe -2
    }

    test("negate flips the sign with integer overflow") {
        evaluate(primitives, """{"type":"minecraft:negate","input":5}""") shouldBe -5
        evaluate(primitives, """{"type":"minecraft:negate","input":-2147483648}""") shouldBe Int.MIN_VALUE
    }

    test("pow converts the integral power result to an integer") {
        evaluate(primitives, """{"type":"minecraft:pow","base":-2,"exponent":3}""") shouldBe -8
        evaluate(primitives, """{"type":"minecraft:pow","base":2,"exponent":-1}""") shouldBe 0
    }

    test("sub subtracts the right input from the left") {
        evaluate(primitives, """{"type":"minecraft:sub","left":3,"right":8}""") shouldBe -5
    }
}

private fun evaluate(primitives: DatapackPrimitives, json: String): Int {
    val parsed = primitives.intNumberProviderCodec.decode(Transcoder.JSON, JsonParser.parseString(json))
    val provider = when (parsed) {
        is Result.Ok -> parsed.value
        is Result.Error -> error("Could not decode integer provider: ${parsed.message}")
    }
    return provider.get(DataContext(hashMapOf()), Random(0L))
}
