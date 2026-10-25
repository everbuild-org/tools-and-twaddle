package org.everbuild.twaddle.loot

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.BlockLocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.BlockPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DefaultRandomSequenceController
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.EnchantmentLevelContext
import org.everbuild.twaddle.datapack_primitives.context.ExplosionRadiusContext
import org.everbuild.twaddle.datapack_primitives.context.ItemPredicateMatcherContext
import org.everbuild.twaddle.datapack_primitives.context.KilledByPlayerContext
import org.everbuild.twaddle.datapack_primitives.context.LocationContextKey
import org.everbuild.twaddle.datapack_primitives.context.LocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationPositionPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationRange
import org.everbuild.twaddle.datapack_primitives.context.RandomSequenceController
import org.everbuild.twaddle.datapack_primitives.context.RandomSequenceControllerContext
import org.everbuild.twaddle.datapack_primitives.context.RegistryEntryReference
import org.everbuild.twaddle.datapack_primitives.context.RegistryEntrySet
import org.everbuild.twaddle.datapack_primitives.context.ToolPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.createEntityPredicateCodec

private fun rawValue(json: String): Codec.RawValue = when (
    val result = Codec.RAW_VALUE.decode(Transcoder.JSON, JsonParser.parseString(json))
) {
    is Result.Ok -> result.value
    is Result.Error -> error("Could not decode raw JSON value: ${result.message}")
}

private fun decodePigPredicate(): EntityPredicate {
    val codec = createEntityPredicateCodec(DatapackPrimitives.vanilla().entityPredicateSubPredicateTypes)
    return when (val result = codec.decode(Transcoder.JSON, JsonParser.parseString("""
        {"minecraft:entity_type":"minecraft:pig"}
    """))) {
        is Result.Ok -> result.value
        is Result.Error -> error("Could not decode entity predicate: ${result.message}")
    }
}

val VanillaLootContextTests by testSuite("Vanilla loot contexts") {
    envTest("block-break contexts connect Minestom matchers and world-backed location checks") { env ->
        val instance = env.createEmptyInstance()
        val position = BlockVec(2, 3, 4)
        instance.loadChunk(position.chunkX(), position.chunkZ()).join()
        instance.setBlock(position, Block.STONE)
        val entity = Entity(EntityType.PIG)
        val randomSequences: RandomSequenceController = DefaultRandomSequenceController()
        val tool = ItemStack.of(Material.IRON_PICKAXE)

        val context = DataContext.blockBreak(
            instance = instance,
            block = Block.STONE,
            blockPosition = position,
            tool = tool,
            entity = entity,
            randomSequences = randomSequences,
            lootLuck = 1.5f,
            explosionRadius = 3f,
        )

        context[LocationContextKey].origin shouldBe ContextLocation(2.5, 3.5, 4.5)
        context[RandomSequenceControllerContext] shouldBe randomSequences
        context[LootLuckContext] shouldBe 1.5f
        context[ExplosionRadiusContext] shouldBe 3f
        context[KilledByPlayerContext] shouldBe false
        context[EnchantmentLevelContext]?.enchantments()?.entries?.isEmpty() shouldBe true

        val itemPredicate = rawValue("""{"items":"minecraft:iron_pickaxe"}""")
        context[ItemPredicateMatcherContext].matches(tool, itemPredicate) shouldBe true
        context[ItemPredicateMatcherContext].matches(ItemStack.of(Material.STICK), itemPredicate) shouldBe false
        context[ToolPredicateContext].matches(itemPredicate) shouldBe true

        val blockPredicate = mapOf("blocks" to rawValue("\"minecraft:stone\""))
        context[BlockPredicateContext].matches(blockPredicate) shouldBe true
        context[BlockPredicateContext].matches(mapOf("blocks" to rawValue("\"minecraft:dirt\""))) shouldBe false

        context[EntityPredicateContext].matches("this", decodePigPredicate()) shouldBe true
        context[EntityPredicateContext].matches("missing", decodePigPredicate()) shouldBe false

        val location = context[LocationContextKey]
        val atStone = LocationPredicate(
            position = LocationPositionPredicate(x = LocationRange(2.5, 2.5), y = null, z = null),
            block = BlockLocationPredicate(
                blocks = RegistryEntrySet(listOf(RegistryEntryReference(Key.key("minecraft:stone")))),
            ),
        )
        location.evaluator.matches(location.origin, atStone) shouldBe true
        location.evaluator.matches(
            location.origin,
            atStone.copy(position = LocationPositionPredicate(x = LocationRange(8.0, 9.0), y = null, z = null)),
        ) shouldBe false
    }
}
