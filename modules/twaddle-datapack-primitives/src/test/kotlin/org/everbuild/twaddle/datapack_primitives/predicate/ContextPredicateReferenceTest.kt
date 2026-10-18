package org.everbuild.twaddle.datapack_primitives.predicate

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.withContextPredicateResolver
import java.util.Random

private val predicateCodec = DatapackPrimitives.vanilla().contextPredicateCodec

private data object ResolvedPredicate : ContextPredicate {
    override fun type() = Key.key("test:resolved")
    override fun test(context: DataContext, random: Random) = false
}

private fun decodePredicate(json: String): ContextPredicate = when (
    val result = predicateCodec.decode(Transcoder.JSON, JsonParser.parseString(json))
) {
    is Result.Ok -> result.value
    is Result.Error -> error("Could not decode context predicate: ${result.message}")
}

val contextPredicateReferenceTests by testSuite("Context-predicate references") {
    test("named references decode as keys and resolve during evaluation") {
        val reference = decodePredicate("\"test:example\"")
        val result = reference.test(
            DataContext(hashMapOf()).withContextPredicateResolver { key ->
                key shouldBe Key.key("test:example")
                ResolvedPredicate
            },
            Random(0),
        )

        result shouldBe false
    }
}
