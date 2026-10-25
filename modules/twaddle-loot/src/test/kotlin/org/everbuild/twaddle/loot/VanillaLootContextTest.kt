package org.everbuild.twaddle.loot

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
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
import org.everbuild.twaddle.datapack_primitives.context.FluidLocationPredicate
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
    return decodeEntityPredicate("""
        {"minecraft:entity_type":"minecraft:pig"}
    """)
}

private fun decodeEntityPredicate(json: String): EntityPredicate {
    val codec = createEntityPredicateCodec(DatapackPrimitives.vanilla().entityPredicateSubPredicateTypes)
    return when (val result = codec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
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
            structureMatcher = StructurePredicateMatcher { _, structure ->
                structure.key == Key.key("test:stronghold")
            },
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
        location.evaluator.matches(
            location.origin,
            atStone.copy(structures = RegistryEntrySet(listOf(RegistryEntryReference(Key.key("test:stronghold"))))),
        ) shouldBe true
        location.evaluator.matches(
            location.origin,
            atStone.copy(structures = RegistryEntrySet(listOf(RegistryEntryReference(Key.key("test:other"))))),
        ) shouldBe false
    }

    envTest("entity matchers evaluate distance, movement, cadence, tags, and vehicle roles") { env ->
        val instance = env.createEmptyInstance()
        val pig = Entity(EntityType.PIG)
        val context = DataContext.blockBreak(
            instance = instance,
            block = Block.STONE,
            blockPosition = BlockVec(2, 3, 4),
            entityTargets = mapOf("this" to pig),
        )
        val matcher = context[EntityPredicateContext]

        matcher.matches(
            "this",
            decodeEntityPredicate("""
                {"minecraft:distance":{"x":2.5,"y":3.5,"z":4.5}}
            """),
        ) shouldBe true
        matcher.matches(
            "this",
            decodeEntityPredicate("""
                {"minecraft:distance":{"x":{"max":2.0}}}
            """),
        ) shouldBe false
        matcher.matches("this", decodeEntityPredicate("""{"minecraft:periodic_tick":1}""")) shouldBe true
        matcher.matches(
            "this",
            decodeEntityPredicate("""
                {"minecraft:movement":{"x":0.0,"y":0.0,"z":0.0,"speed":0.0}}
            """),
        ) shouldBe true
        matcher.matches(
            "this",
            decodeEntityPredicate("""{"minecraft:entity_tags":{"none_of":["captain"]}}"""),
        ) shouldBe true
        matcher.matches("this", decodeEntityPredicate("""{"minecraft:passengers":[]}""")) shouldBe true
        matcher.matches(
            "this",
            decodeEntityPredicate("""
                {"minecraft:passengers":[{"minecraft:entity_type":"minecraft:pig"}]}
            """),
        ) shouldBe false
        matcher.matches(
            "this",
            decodeEntityPredicate("""
                {"minecraft:vehicle":{"minecraft:entity_type":"minecraft:pig"}}
            """),
        ) shouldBe false
    }

    envTest("location predicates evaluate fluid, light, biome, sky visibility, and smoke") { env ->
        val instance = env.createEmptyInstance()
        val position = BlockVec(2, 3, 4)
        instance.loadChunk(position.chunkX(), position.chunkZ()).join()
        val water = requireNotNull(Block.fromKey(Key.key("minecraft:water")))
        instance.setBlock(position, water)
        val context = DataContext.blockBreak(instance, water, position)
        val location = context[LocationContextKey]
        val biome = instance.getBiome(position.blockX(), position.blockY(), position.blockZ()).key()
        val fluidPredicate = LocationPredicate(
            dimension = instance.dimensionType.key(),
            biomes = RegistryEntrySet(listOf(RegistryEntryReference(biome))),
            light = LocationRange(0, 15),
            fluid = FluidLocationPredicate(
                fluids = RegistryEntrySet(listOf(RegistryEntryReference(Key.key("minecraft:water")))),
            ),
        )

        location.evaluator.matches(location.origin, fluidPredicate) shouldBe true
        location.evaluator.matches(location.origin, fluidPredicate.copy(dimension = Key.key("minecraft:the_nether"))) shouldBe false
        location.evaluator.matches(
            location.origin,
            fluidPredicate.copy(fluid = FluidLocationPredicate(
                fluids = RegistryEntrySet(listOf(RegistryEntryReference(Key.key("minecraft:lava")))),
            )),
        ) shouldBe false

        location.evaluator.matches(location.origin, LocationPredicate(canSeeSky = true)) shouldBe true
        instance.setBlock(BlockVec(position.blockX(), position.blockY() + 1, position.blockZ()), Block.STONE)
        location.evaluator.matches(location.origin, LocationPredicate(canSeeSky = true)) shouldBe false

        val campfire = requireNotNull(Block.fromKey(Key.key("minecraft:campfire"))).withProperty("lit", "true")
        instance.setBlock(BlockVec(position.blockX(), position.blockY() - 1, position.blockZ()), campfire)
        location.evaluator.matches(location.origin, LocationPredicate(smokey = true)) shouldBe true
        instance.setBlock(BlockVec(position.blockX(), position.blockY() - 1, position.blockZ()), Block.DIRT)
        location.evaluator.matches(location.origin, LocationPredicate(smokey = true)) shouldBe false
    }

    envTest("container, interaction, and entity-death contexts expose their vanilla roles") { env ->
        val instance = env.createEmptyInstance()
        val position = BlockVec(4, 5, 6)
        val opener = Entity(EntityType.PIG)

        val container = DataContext.containerLoot(instance, position, opener = opener)
        container[LocationContextKey].origin shouldBe ContextLocation(4.5, 5.5, 6.5)
        container[EntityPredicateContext].matches("this", decodePigPredicate()) shouldBe true

        val tool = ItemStack.of(Material.IRON_PICKAXE)
        val interaction = DataContext.blockInteract(
            instance = instance,
            block = Block.STONE,
            blockPosition = position,
            interactingEntity = opener,
            tool = tool,
        )
        interaction[EntityPredicateContext].matches("interacting_entity", decodePigPredicate()) shouldBe true
        interaction[EntityPredicateContext].matches("this", decodePigPredicate()) shouldBe false
        interaction[ToolPredicateContext].matches(rawValue("""{"items":"minecraft:iron_pickaxe"}""")) shouldBe true

        val deceased = Entity(EntityType.PIG)
        deceased.setInstance(instance, Pos(8.0, 9.0, 10.0)).join()
        val death = DataContext.entityDeath(entity = deceased)
        death[LocationContextKey].origin shouldBe ContextLocation(8.0, 9.0, 10.0)
        death[KilledByPlayerContext] shouldBe false
        death[EntityPredicateContext].matches("this", decodePigPredicate()) shouldBe true
    }
}
