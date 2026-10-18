package org.everbuild.twaddle.datapack_primitives.predicate

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateValue
import org.everbuild.twaddle.datapack_primitives.context.ScoreAccessor
import org.everbuild.twaddle.datapack_primitives.context.ScoreTarget
import org.everbuild.twaddle.datapack_primitives.context.TeamPredicate
import org.everbuild.twaddle.datapack_primitives.context.withBlockPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.withDamageSourcePredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.withEntityPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.withKilledByPlayer
import org.everbuild.twaddle.datapack_primitives.context.withScores
import org.everbuild.twaddle.datapack_primitives.context.withToolPredicateMatcher
import java.util.Random

private val predicateCodec = DatapackPrimitives.vanilla().contextPredicateCodec

private fun predicate(json: String): ContextPredicate =
    when (val decoded = predicateCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode context predicate: ${decoded.message}")
    }

private fun evaluate(predicateJson: String, context: DataContext) =
    predicate(predicateJson).test(context, Random(0))

val matcherPredicateEvaluationTests by testSuite("Matcher-backed context predicate evaluation") {
    test("entity properties pass the named entity and decoded predicate to its matcher") {
        val entityMatcher = org.everbuild.twaddle.datapack_primitives.context.EntityPredicateMatcher { target, entityPredicate ->
            target shouldBe "this"
            val team = entityPredicate.subPredicates[Key.key("minecraft:team")] as EntityPredicateValue.SubPredicate
            (team.value as TeamPredicate).team shouldBe "builders"
            true
        }

        evaluate(
            """{"type":"minecraft:entity_properties","entity":"this","predicate":{"minecraft:team":"builders"}}""",
            DataContext(hashMapOf()).withEntityPredicateMatcher(entityMatcher),
        ) shouldBe true
        evaluate(
            """{"type":"minecraft:entity_properties","entity":"killer"}""",
            DataContext(hashMapOf()),
        ) shouldBe true
    }

    test("entity scores compare target-aware scores against exact and bounded values") {
        val accessor = object : ScoreAccessor {
            override fun getScore(objective: String): Int = error("The entity target should be provided")

            override fun getScore(target: ScoreTarget, objective: String): Int? {
                target shouldBe ScoreTarget("this", null)
                return when (objective) {
                    "kills" -> 7
                    "deaths" -> 12
                    else -> null
                }
            }
        }
        val context = DataContext(hashMapOf()).withScores(accessor)

        evaluate(
            """{"type":"minecraft:entity_scores","entity":"this","scores":{"kills":7,"deaths":{"min":10,"max":15}}}""",
            context,
        ) shouldBe true
        evaluate(
            """{"type":"minecraft:entity_scores","entity":"this","scores":{"kills":{"min":8}}}""",
            context,
        ) shouldBe false
        evaluate(
            """{"type":"minecraft:entity_scores","entity":"this","scores":{"missing":0}}""",
            context,
        ) shouldBe false
    }

    test("killed_by_player reflects the loot context value") {
        evaluate(
            """{"type":"minecraft:killed_by_player"}""",
            DataContext(hashMapOf()).withKilledByPlayer(true),
        ) shouldBe true
        evaluate(
            """{"type":"minecraft:killed_by_player"}""",
            DataContext(hashMapOf()).withKilledByPlayer(false),
        ) shouldBe false
    }

    test("match_block forwards its supplied fields to the block matcher") {
        evaluate(
            """{"type":"minecraft:match_block","blocks":["minecraft:stone"],"state":{"axis":"x"},"components":{"minecraft:custom_name":"Test"}}""",
            DataContext(hashMapOf()).withBlockPredicateMatcher { fields ->
                fields.keys shouldBe setOf("blocks", "state", "components")
                true
            },
        ) shouldBe true
        evaluate(
            """{"type":"minecraft:match_block","blocks":["minecraft:stone"]}""",
            DataContext(hashMapOf()).withBlockPredicateMatcher { false },
        ) shouldBe false
    }

    test("match_tool and damage_source_properties return their matcher's result") {
        evaluate(
            """{"type":"minecraft:match_tool","predicate":{"items":["minecraft:iron_pickaxe"]}}""",
            DataContext(hashMapOf()).withToolPredicateMatcher { true },
        ) shouldBe true
        evaluate(
            """{"type":"minecraft:match_tool","predicate":{"items":["minecraft:iron_pickaxe"]}}""",
            DataContext(hashMapOf()).withToolPredicateMatcher { false },
        ) shouldBe false
        evaluate(
            """{"type":"minecraft:damage_source_properties","predicate":{"tags":[{"id":"minecraft:is_fire","expected":true}]}}""",
            DataContext(hashMapOf()).withDamageSourcePredicateMatcher { true },
        ) shouldBe true
        evaluate(
            """{"type":"minecraft:damage_source_properties","predicate":{"tags":[{"id":"minecraft:is_fire","expected":true}]}}""",
            DataContext(hashMapOf()).withDamageSourcePredicateMatcher { false },
        ) shouldBe false
    }
}
