package org.everbuild.twaddle.datapack_primitives.number_provider

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.floats.shouldBeExactly
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import net.minestom.server.world.attribute.EnvironmentAttribute
import net.minestom.server.world.attribute.EnvironmentAttributeMap
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.ScoreAccessor
import org.everbuild.twaddle.datapack_primitives.context.ScoreTarget
import org.everbuild.twaddle.datapack_primitives.context.StorageAccessor
import org.everbuild.twaddle.datapack_primitives.context.withEnchantment
import org.everbuild.twaddle.datapack_primitives.context.withEnvironment
import org.everbuild.twaddle.datapack_primitives.context.withScores
import org.everbuild.twaddle.datapack_primitives.context.withStorage
import java.util.Random

val contextNumberProviderEvaluationTests by testSuite("Context-backed number provider evaluation") {
    val primitives = DatapackPrimitives.vanilla()

    test("environment attributes use the supplied value and the attribute default") {
        val numericAttribute = EnvironmentAttribute.SKY_LIGHT_LEVEL
        val provider = decode(
            primitives.floatNumberProviderCodec,
            """{"type":"minecraft:environment_attribute","attribute":"minecraft:gameplay/sky_light_level"}""",
        )

        provider.get(
            DataContext(hashMapOf()).withEnvironment(EnvironmentAttributeMap.builder().set(numericAttribute, 4.5f).build()),
            Random(1),
        ) shouldBeExactly 4.5f
        provider.get(DataContext(hashMapOf()).withEnvironment(EnvironmentAttributeMap.EMPTY), Random(1)) shouldBeExactly
            numericAttribute.defaultValue()
        shouldThrow<IllegalStateException> { provider.get(DataContext(hashMapOf()), Random(1)) }
    }

    test("score providers read the requested target and use fallback when no score exists") {
        val provider = decode(
            primitives.intNumberProviderCodec,
            """{"type":"minecraft:score","score":"kills","target":{"type":"fixed","name":"alice"},"fallback":7}""",
        )
        val accessor = object : ScoreAccessor {
            override fun getScore(objective: String) = error("target-aware lookup should be used")
            override fun getScore(target: ScoreTarget, objective: String): Int? {
                target shouldBe ScoreTarget("fixed", "alice")
                objective shouldBe "kills"
                return 12
            }
        }

        provider.get(DataContext(hashMapOf()).withScores(accessor), Random(1)) shouldBe 12
        provider.get(
            DataContext(hashMapOf()).withScores(object : ScoreAccessor {
                override fun getScore(objective: String) = 0
                override fun getScore(target: ScoreTarget, objective: String): Int? = null
            }),
            Random(1),
        ) shouldBe 7
        shouldThrow<IllegalStateException> { provider.get(DataContext(hashMapOf()), Random(1)) }
    }

    test("storage providers read values and fall back to a provider or zero when the path is absent") {
        val provider = decode(
            primitives.intNumberProviderCodec,
            """{"type":"minecraft:storage","storage":"example:values","path":"count","fallback":9}""",
        )
        val requested = mutableListOf<Pair<Key, String>>()
        val accessor = object : StorageAccessor<Int> {
            override fun getValue(value: String): Int? = error("storage-aware lookup should be used")
            override fun getValue(storage: Key, path: String): Int? {
                requested += storage to path
                return 23
            }
        }

        provider.get(DataContext(hashMapOf()).withStorage(accessor), Random(1)) shouldBe 23
        requested shouldBe listOf(Key.key("example:values") to "count")
        provider.get(
            DataContext(hashMapOf()).withStorage(object : StorageAccessor<Int> {
                override fun getValue(value: String): Int? = null
                override fun getValue(storage: Key, path: String): Int? = null
            }),
            Random(1),
        ) shouldBe 9
        decode(
            primitives.intNumberProviderCodec,
            """{"type":"minecraft:storage","storage":"example:values","path":"absent"}""",
        ).get(
            DataContext(hashMapOf()).withStorage(object : StorageAccessor<Int> {
                override fun getValue(value: String): Int? = null
            }),
            Random(1),
        ) shouldBe 0
        shouldThrow<IllegalStateException> { provider.get(DataContext(hashMapOf()), Random(1)) }
    }

    test("enchantment level providers use seeded selection and return the base for empty enchantments") {
        val provider = decode(
            primitives.floatNumberProviderCodec,
            """{"type":"minecraft:enchantment_level","amount":{"type":"minecraft:linear","base":1.0,"per_level_above_first":0.25}}""",
        )
        val enchantments = EnchantmentList(
            linkedMapOf(
                RegistryKey.of<Enchantment>(Key.key("minecraft:sharpness")) to 2,
                RegistryKey.of<Enchantment>(Key.key("minecraft:efficiency")) to 5,
            ),
        )

        val seededResult = provider.get(DataContext(hashMapOf()).withEnchantment(enchantments), Random(4))
        listOf(1.25f, 2.0f) shouldContain seededResult
        provider.get(DataContext(hashMapOf()).withEnchantment(enchantments), Random(4)) shouldBeExactly seededResult
        provider.get(DataContext(hashMapOf()).withEnchantment(null), Random(4)) shouldBeExactly 1.0f
        shouldThrow<IllegalStateException> { provider.get(DataContext(hashMapOf()), Random(4)) }
    }
}

private fun <T> decode(codec: net.minestom.server.codec.Codec<NumberProviderDeclaration<T>>, json: String): NumberProviderDeclaration<T> =
    when (val result = codec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> result.value
        is Result.Error -> error("Could not decode number provider: ${result.message}")
    }
