package org.everbuild.twaddle.datapack_primitives.context

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives

private val entityPredicateCodec = createEntityPredicateCodec(
    DatapackPrimitives.vanilla().entityPredicateSubPredicateTypes,
)

private val allEntityPredicateTypes = """
    {
      "minecraft:entity_type": "minecraft:pig",
      "minecraft:location": {"position": {"x": {"min": 1.0, "max": 3.0}}},
      "minecraft:stepping_on": {},
      "minecraft:team": "red",
      "minecraft:periodic_tick": 20,
      "minecraft:entity_tags": {"any_of": ["a"], "all_of": ["b"], "none_of": ["c"]},
      "minecraft:distance": {"absolute": {"min": 1.0, "max": 5.0}, "horizontal": 3.0},
      "minecraft:flags": {"is_baby": false, "is_on_ground": true},
      "minecraft:effects": {
        "minecraft:speed": {
          "amplifier": 1,
          "duration": {"min": 20},
          "ambient": false,
          "visible": true,
          "show_particles": true
        }
      },
      "minecraft:movement": {"x": 0.1, "speed": {"min": 0.1, "max": 1.0}},
      "minecraft:passengers": [{"minecraft:team": "red"}],
      "minecraft:targeted_entity": {"minecraft:periodic_tick": 10},
      "minecraft:vehicle": {"minecraft:team": "blue"},
      "minecraft:equipment": {"mainhand": {"items": "minecraft:diamond_sword"}},
      "minecraft:slots": {"weapon.mainhand": {"items": "minecraft:diamond_sword"}},
      "minecraft:type_specific/fishing_hook": {"in_open_water": true},
      "minecraft:type_specific/lightning": {
        "blocks_set_on_fire": 1,
        "entity_struck": {"minecraft:team": "red"}
      },
      "minecraft:type_specific/raider": {"is_captain": true, "has_raid": false},
      "minecraft:type_specific/sheep": {"sheared": true},
      "minecraft:type_specific/slime": {"size": {"min": 2, "max": 4}},
      "test:raw": {"custom": true}
    }
""".trimIndent()

private fun decodeEntityPredicate(json: String): EntityPredicate = when (
    val result = entityPredicateCodec.decode(Transcoder.JSON, JsonParser.parseString(json))
) {
    is Result.Ok -> result.value
    is Result.Error -> error("Could not decode entity predicate: ${result.message}")
}

val EntityPredicateCodecTests by testSuite("Entity-predicate sub-predicate codecs") {
    test("every registered vanilla sub-predicate decodes to its typed model") {
        val predicate = decodeEntityPredicate(allEntityPredicateTypes)

        val typedValues = predicate.subPredicates.values.mapNotNull { value ->
            (value as? EntityPredicateValue.SubPredicate)?.value
        }
        typedValues.size shouldBe 20
        predicate.subPredicates[Key.key("test:raw")].shouldBeInstanceOf<EntityPredicateValue.Raw>()

        typedValues.filterIsInstance<EntityTypePredicate>().single().types.values.single().key shouldBe
            Key.key("minecraft:pig")
        typedValues.filterIsInstance<LocationEntityPredicate>().size shouldBe 2
        typedValues.filterIsInstance<TeamPredicate>().size shouldBe 1
        typedValues.filterIsInstance<PeriodicTickPredicate>().single().period shouldBe 20
        typedValues.filterIsInstance<EntityTagsPredicate>().single().allOf shouldBe listOf("b")
        typedValues.filterIsInstance<EntityDistancePredicate>().single().horizontal shouldBe LocationRange(3.0, 3.0)
        typedValues.filterIsInstance<EntityFlagsPredicate>().single().isOnGround shouldBe true
        typedValues.filterIsInstance<EntityEffectsPredicate>().single().effects.keys shouldBe
            setOf(Key.key("minecraft:speed"))
        typedValues.filterIsInstance<EntityMovementPredicate>().single().x shouldBe LocationRange(0.1, 0.1)
        typedValues.filterIsInstance<EntityPassengersPredicate>().single().passengers.size shouldBe 1
        typedValues.filterIsInstance<EntityNestedPredicate>().size shouldBe 2
        typedValues.filterIsInstance<EntityEquipmentPredicate>().single().equipment.keys shouldBe setOf("mainhand")
        typedValues.filterIsInstance<EntitySlotsPredicate>().single().slots.keys shouldBe setOf("weapon.mainhand")
        typedValues.filterIsInstance<FishingHookPredicate>().single().inOpenWater shouldBe true
        typedValues.filterIsInstance<LightningPredicate>().single().blocksSetOnFire shouldBe LocationRange(1, 1)
        typedValues.filterIsInstance<RaiderPredicate>().single().hasRaid shouldBe false
        typedValues.filterIsInstance<SheepPredicate>().single().sheared shouldBe true
        typedValues.filterIsInstance<SlimePredicate>().single().size shouldBe LocationRange(2, 4)
    }

    test("all vanilla sub-predicate codecs round-trip together, including recursive and raw values") {
        val decoded = decodeEntityPredicate(allEntityPredicateTypes)
        val encoded = entityPredicateCodec.encode(Transcoder.JSON, decoded).orElseThrow()

        val roundTripped = when (val result = entityPredicateCodec.decode(Transcoder.JSON, encoded)) {
            is Result.Ok -> result.value
            is Result.Error -> error("Could not decode encoded entity predicate: ${result.message}")
        }

        roundTripped shouldBe decoded
    }
}
