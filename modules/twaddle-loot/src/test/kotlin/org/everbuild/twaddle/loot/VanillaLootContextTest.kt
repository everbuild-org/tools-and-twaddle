package org.everbuild.twaddle.loot

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.nbt.CompoundBinaryTag
import net.kyori.adventure.text.Component
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.Entity
import net.minestom.server.entity.EntityType
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.LivingEntity
import net.minestom.server.entity.damage.Damage
import net.minestom.server.entity.damage.DamageType
import net.minestom.server.entity.metadata.animal.SheepMeta
import net.minestom.server.entity.metadata.cube.AbstractCubeMeta
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.potion.Potion
import net.minestom.server.potion.PotionEffect
import net.minestom.server.registry.RegistryKey
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.BlockLocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.BlockPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DefaultRandomSequenceController
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicate
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateValue
import org.everbuild.twaddle.datapack_primitives.context.EnchantmentLevelContext
import org.everbuild.twaddle.datapack_primitives.context.ExplosionRadiusContext
import org.everbuild.twaddle.datapack_primitives.context.FluidLocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.ItemPredicateMatcherContext
import org.everbuild.twaddle.datapack_primitives.context.KilledByPlayerContext
import org.everbuild.twaddle.datapack_primitives.context.LocationContextKey
import org.everbuild.twaddle.datapack_primitives.context.LocationEntityPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationPositionPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationRange
import org.everbuild.twaddle.datapack_primitives.context.RandomSequenceController
import org.everbuild.twaddle.datapack_primitives.context.RandomSequenceControllerContext
import org.everbuild.twaddle.datapack_primitives.context.RegistryEntryReference
import org.everbuild.twaddle.datapack_primitives.context.RegistryEntrySet
import org.everbuild.twaddle.datapack_primitives.context.ToolPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.createEntityPredicateCodec
import org.everbuild.twaddle.loot.modifier.context.LootComponentSourceAccessContext
import java.util.Random

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

    envTest("entity matchers combine components, flags, equipment, and unordered passengers") { env ->
        val instance = env.createEmptyInstance()
        instance.loadChunk(0, 0).join()

        val vehicle = Entity(EntityType.HORSE)
        vehicle.setInstance(instance, Pos.ZERO).join()
        val pigPassenger = Entity(EntityType.PIG)
        val sheepPassenger = Entity(EntityType.SHEEP)
        vehicle.addPassenger(pigPassenger)
        vehicle.addPassenger(sheepPassenger)

        val equippedPig = LivingEntity(EntityType.PIG)
        equippedPig.setEquipment(EquipmentSlot.MAIN_HAND, ItemStack.of(Material.IRON_SWORD))
        equippedPig.set(DataComponents.CUSTOM_NAME, Component.text("named pig"))
        equippedPig.addEffect(
            Potion(
                PotionEffect.fromKey("minecraft:speed") ?: error("Missing vanilla speed effect"),
                2,
                200,
                Potion.AMBIENT_FLAG.toInt() or Potion.ICON_FLAG.toInt(),
            ),
        )
        val matcher = DataContext.blockBreak(
            instance = instance,
            block = Block.STONE,
            blockPosition = BlockVec.ZERO,
            entityTargets = mapOf("vehicle" to vehicle, "equipped" to equippedPig, "rider" to pigPassenger),
        )[EntityPredicateContext]

        matcher.matches(
            "vehicle",
            decodeEntityPredicate("""
                {"minecraft:passengers":[
                  {"minecraft:entity_type":"minecraft:sheep"},
                  {"minecraft:entity_type":"minecraft:pig"}
                ]}
            """),
        ) shouldBe true
        matcher.matches(
            "vehicle",
            decodeEntityPredicate("""
                {"minecraft:passengers":[
                  {"minecraft:entity_type":"minecraft:pig"},
                  {"minecraft:entity_type":"minecraft:pig"}
                ]}
            """),
        ) shouldBe false
        matcher.matches(
            "rider",
            decodeEntityPredicate("""{"minecraft:vehicle":{"minecraft:entity_type":"minecraft:horse"}}"""),
        ) shouldBe true

        matcher.matches(
            "equipped",
            decodeEntityPredicate("""{"minecraft:flags":{"is_flying":false,"is_gliding":false}}"""),
        ) shouldBe true
        matcher.matches(
            "equipped",
            decodeEntityPredicate("""{"minecraft:equipment":{"mainhand":{"items":"minecraft:iron_sword"}}}"""),
        ) shouldBe true
        matcher.matches(
            "equipped",
            decodeEntityPredicate("""{"minecraft:slots":{"main_hand":{"items":"minecraft:iron_sword"}}}"""),
        ) shouldBe true
        matcher.matches(
            "equipped",
            decodeEntityPredicate("""
                {"minecraft:effects":{"minecraft:speed":{
                  "amplifier":2,"duration":{"min":200,"max":200},
                  "ambient":true,"visible":true,"show_particles":false
                }}}
            """),
        ) shouldBe true
        matcher.matches(
            "equipped",
            decodeEntityPredicate("""{"minecraft:effects":{"minecraft:strength":{}}}"""),
        ) shouldBe false

        val customName = DataComponents.CUSTOM_NAME
        val customNameKey = Key.key("minecraft:custom_name")
        val matchingName = EntityPredicate(
            mapOf(
                customNameKey to EntityPredicateValue.Component(
                    customNameKey,
                    customName,
                    Component.text("named pig"),
                ),
            ),
        )
        matcher.matches("equipped", matchingName) shouldBe true
        matcher.matches(
            "equipped",
            matchingName.copy(
                subPredicates = mapOf(
                    customNameKey to EntityPredicateValue.Component(
                        customNameKey,
                        customName,
                        Component.text("different name"),
                    ),
                ),
            ),
        ) shouldBe false

        val locationKey = Key.key("minecraft:location")
        val atOrigin = EntityPredicate(
            mapOf(
                locationKey to EntityPredicateValue.SubPredicate(
                    locationKey,
                    LocationEntityPredicate(
                        LocationPredicate(
                            position = LocationPositionPredicate(x = LocationRange(0.0, 0.0), y = null, z = null),
                        ),
                    ),
                ),
            ),
        )
        matcher.matches("equipped", atOrigin) shouldBe true
    }

    envTest("entity type-specific predicates report supported state and unsupported vanilla fields") { env ->
        val instance = env.createEmptyInstance()
        val sheep = Entity(EntityType.SHEEP)
        (sheep.entityMeta as SheepMeta).setSheared(true)
        val slime = Entity(EntityType.SLIME)
        (slime.entityMeta as AbstractCubeMeta).setSize(4)
        val hook = Entity(EntityType.FISHING_BOBBER)
        val raider = Entity(EntityType.PILLAGER)
        val lightning = Entity(EntityType.LIGHTNING_BOLT)
        val pig = Entity(EntityType.PIG)
        val matcher = DataContext.blockBreak(
            instance = instance,
            block = Block.STONE,
            blockPosition = BlockVec.ZERO,
            entityTargets = mapOf(
                "sheep" to sheep,
                "slime" to slime,
                "hook" to hook,
                "raider" to raider,
                "lightning" to lightning,
                "pig" to pig,
            ),
        )[EntityPredicateContext]

        matcher.matches("sheep", decodeEntityPredicate("""{"minecraft:type_specific/sheep":{"sheared":true}}""")) shouldBe true
        matcher.matches("sheep", decodeEntityPredicate("""{"minecraft:type_specific/sheep":{"sheared":false}}""")) shouldBe false
        matcher.matches("slime", decodeEntityPredicate("""{"minecraft:type_specific/slime":{"size":4}}""")) shouldBe true
        matcher.matches("hook", decodeEntityPredicate("""{"minecraft:type_specific/fishing_hook":{}}""")) shouldBe true
        matcher.matches("raider", decodeEntityPredicate("""{"minecraft:type_specific/raider":{}}""")) shouldBe true
        matcher.matches("lightning", decodeEntityPredicate("""{"minecraft:type_specific/lightning":{}}""")) shouldBe true
        matcher.matches("pig", decodeEntityPredicate("""{"minecraft:team":"missing-team"}""")) shouldBe false

        shouldThrow<IllegalStateException> {
            matcher.matches(
                "hook",
                decodeEntityPredicate("""{"minecraft:type_specific/fishing_hook":{"in_open_water":true}}"""),
            )
        }
        shouldThrow<IllegalStateException> {
            matcher.matches(
                "raider",
                decodeEntityPredicate("""{"minecraft:type_specific/raider":{"has_raid":true}}"""),
            )
        }
        shouldThrow<IllegalStateException> {
            matcher.matches(
                "lightning",
                decodeEntityPredicate("""{"minecraft:type_specific/lightning":{"blocks_set_on_fire":1}}"""),
            )
        }
        shouldThrow<IllegalStateException> {
            matcher.matches(
                "pig",
                decodeEntityPredicate("""{"minecraft:movement":{"fall_distance":1.0}}"""),
            )
        }
        shouldThrow<IllegalStateException> {
            matcher.matches("pig", decodeEntityPredicate("""{"minecraft:unknown_modded_field":{}}"""))
        }
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
        location.evaluator.matches(
            location.origin,
            fluidPredicate.copy(
                fluid = fluidPredicate.fluid?.copy(state = mapOf("level" to LocationRange("0", "0"))),
            ),
        ) shouldBe true
        location.evaluator.matches(
            location.origin,
            fluidPredicate.copy(
                fluid = fluidPredicate.fluid?.copy(state = mapOf("level" to LocationRange("1", "1"))),
            ),
        ) shouldBe false
        location.evaluator.matches(
            location.origin,
            fluidPredicate.copy(
                fluid = fluidPredicate.fluid?.copy(state = mapOf("level" to LocationRange("not-a-number", null))),
            ),
        ) shouldBe false
        location.evaluator.matches(location.origin, fluidPredicate.copy(dimension = Key.key("minecraft:the_nether"))) shouldBe false
        location.evaluator.matches(
            location.origin,
            fluidPredicate.copy(fluid = FluidLocationPredicate(
                fluids = RegistryEntrySet(listOf(RegistryEntryReference(Key.key("minecraft:lava")))),
            )),
        ) shouldBe false

        val withoutStructureAccess = DataContext.blockBreak(instance, water, position)[LocationContextKey]
        shouldThrow<IllegalStateException> {
            withoutStructureAccess.evaluator.matches(
                withoutStructureAccess.origin,
                LocationPredicate(
                    structures = RegistryEntrySet(listOf(RegistryEntryReference(Key.key("test:stronghold")))),
                ),
            )
        }

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

        val attacker = Entity(EntityType.PIG)
        val projectile = Entity(EntityType.ARROW)
        val damage = Damage(RegistryKey.of<DamageType>(Key.key("minecraft:player_attack")), projectile, attacker, null, 1f)
        val damagedDeath = DataContext.entityDeath(entity = deceased, damage = damage)
        damagedDeath[EntityPredicateContext].matches("attacker", decodePigPredicate()) shouldBe true
        damagedDeath[EntityPredicateContext].matches("killer", decodePigPredicate()) shouldBe true
        val projectilePredicate = decodeEntityPredicate("""{"minecraft:entity_type":"minecraft:arrow"}""")
        damagedDeath[EntityPredicateContext].matches("direct_attacker", projectilePredicate) shouldBe true
        damagedDeath[EntityPredicateContext].matches("direct_killer", projectilePredicate) shouldBe true
    }

    envTest("block entity loot sources resolve native contents and reject unknown sources") { env ->
        val instance = env.createEmptyInstance()
        val chest = Block.CHEST.withNbt(
            CompoundBinaryTag.builder()
                .put("components", CompoundBinaryTag.empty())
                .build(),
        )
        val context = DataContext.blockBreak(instance, chest, BlockVec.ZERO)
        val resolver = context[DynamicLootResolverContext]

        context[LootComponentSourceAccessContext].getComponents("block_entity") shouldNotBe null
        context[LootComponentSourceAccessContext].getComponents("unsupported") shouldBe null
        resolver.resolve(Key.key("minecraft:contents"), context, Random(0)) shouldBe emptyList()
        resolver.resolve(Key.key("minecraft:sherds"), context, Random(0)) shouldBe emptyList()
        shouldThrow<IllegalStateException> {
            resolver.resolve(Key.key("test:unknown"), context, Random(0))
        }
    }
}
