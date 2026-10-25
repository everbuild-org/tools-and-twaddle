package org.everbuild.twaddle.loot

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Transcoder
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockTags
import net.minestom.server.instance.block.predicate.BlockPredicate as MinestomBlockPredicate
import net.minestom.server.registry.Registries
import net.minestom.server.registry.Registry
import net.minestom.server.registry.RegistryKey
import net.minestom.server.registry.RegistryTranscoder
import org.everbuild.twaddle.datapack_primitives.context.BlockLocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.FluidLocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationPredicate
import org.everbuild.twaddle.datapack_primitives.context.LocationRange
import org.everbuild.twaddle.datapack_primitives.context.RegistryEntrySet
import org.everbuild.twaddle.datapack_primitives.context.BLOCK_LOCATION_PREDICATE_CODEC
import kotlin.math.floor
import kotlin.math.max

internal class VanillaLocationPredicateEvaluator(
    private val instance: Instance,
    private val registries: Registries,
    private val structureMatcher: StructurePredicateMatcher?,
) {
    private val campfireKeys = setOf(Key.key("minecraft:campfire"), Key.key("minecraft:soul_campfire"))

    fun matches(position: ContextLocation, predicate: LocationPredicate): Boolean {
        if (!matchesCoordinates(position, predicate)) return false

        val dimension = instance.dimensionType
        if (predicate.dimension != null && dimension.key() != predicate.dimension) return false
        predicate.structures?.let { structures ->
            val matcher = structureMatcher
                ?: error("Location predicates using structures need a StructurePredicateMatcher")
            if (structures.values.none { matcher.matches(position, it) }) return false
        }

        val needsBlock = predicate.biomes != null || predicate.light != null || predicate.smokey != null ||
            predicate.canSeeSky != null || predicate.block != null || predicate.fluid != null
        if (!needsBlock) return true

        val blockX = floor(position.x).toInt()
        val blockY = floor(position.y).toInt()
        val blockZ = floor(position.z).toInt()
        val block = blockAt(blockX, blockY, blockZ) ?: return false

        predicate.biomes?.let { biomes ->
            val biomeKey = instance.getBiome(blockX, blockY, blockZ)
            if (!matchesEntrySet(biomes, registries.biome(), biomeKey)) return false
        }
        predicate.light?.let { lightRange ->
            val light = max(instance.getBlockLight(blockX, blockY, blockZ), instance.getSkyLight(blockX, blockY, blockZ))
            if (!lightRange.matches(light)) return false
        }
        predicate.smokey?.let { if (isSmokey(blockX, blockY, blockZ) != it) return false }
        predicate.canSeeSky?.let { if (canSeeSky(blockX, blockY, blockZ) != it) return false }
        predicate.block?.let { if (!matchesBlock(block, it)) return false }
        predicate.fluid?.let { if (!matchesFluid(block, it)) return false }
        return true
    }

    private fun matchesCoordinates(position: ContextLocation, predicate: LocationPredicate): Boolean {
        val coordinates = predicate.position ?: return true
        return coordinates.x.matchesOptional(position.x) &&
            coordinates.y.matchesOptional(position.y) &&
            coordinates.z.matchesOptional(position.z)
    }

    private fun matchesBlock(block: Block, predicate: BlockLocationPredicate): Boolean {
        val json = BLOCK_LOCATION_PREDICATE_CODEC.encode(Transcoder.JSON, predicate).orElseThrow()
        val decoded = MinestomBlockPredicate.CODEC
            .decode(RegistryTranscoder(Transcoder.JSON, registries), json)
            .orElseThrow()
        return decoded.test(block)
    }

    private fun matchesFluid(block: Block, predicate: FluidLocationPredicate): Boolean {
        val fluidKey = fluidKey(block) ?: return false
        val fluid = registries.fluid().get(fluidKey) ?: return false
        val registryKey = registries.fluid().getKey(fluid) ?: return false
        predicate.fluids?.let { if (!matchesEntrySet(it, registries.fluid(), registryKey)) return false }

        val properties = if (block.getProperty("waterlogged") == "true") {
            mapOf("level" to "0")
        } else {
            block.properties()
        }
        return predicate.state.orEmpty().all { (name, range) ->
            properties[name]?.let { matchesState(it, range) } == true
        }
    }

    private fun matchesState(actual: String, range: LocationRange<String>): Boolean {
        val minimum = range.min
        val maximum = range.max
        if (minimum == null && maximum == null) return true
        if (minimum == maximum) return actual == minimum

        val actualNumber = actual.toIntOrNull() ?: return false
        val minimumNumber = minimum?.toIntOrNull() ?: if (minimum == null) null else return false
        val maximumNumber = maximum?.toIntOrNull() ?: if (maximum == null) null else return false
        return (minimumNumber == null || actualNumber >= minimumNumber) &&
            (maximumNumber == null || actualNumber <= maximumNumber)
    }

    private fun fluidKey(block: Block): Key? {
        if (block.getProperty("waterlogged") == "true") return Key.key("minecraft:water")
        val key = block.key()
        if (!block.fluid()) return null
        return key.takeIf { registries.fluid().get(it) != null }
    }

    private fun canSeeSky(x: Int, y: Int, z: Int): Boolean {
        val dimension = instance.cachedDimensionType
        if (!dimension.hasSkylight()) return false
        for (scanY in y + 1 until dimension.minY() + dimension.height()) {
            val block = blockAt(x, scanY, z) ?: return false
            if (blocksMotion(block)) return false
        }
        return true
    }

    private fun isSmokey(x: Int, y: Int, z: Int): Boolean {
        for (distance in 1..24) {
            val campfireY = y - distance
            val campfire = blockAt(x, campfireY, z) ?: return false
            if (campfire.key() !in campfireKeys || campfire.getProperty("lit") != "true") continue

            val hayBale = blockAt(x, campfireY - 1, z)?.key() == Key.key("minecraft:hay_block")
            val maximumDistance = if (hayBale) 24 else 5
            if (distance > maximumDistance) continue
            val smokePathBlocked = (campfireY + 1 until y).any { scanY ->
                blockAt(x, scanY, z)?.let(::blocksMotion) != false
            }
            if (!smokePathBlocked) return true
        }
        return false
    }

    private fun blockAt(x: Int, y: Int, z: Int): Block? {
        val dimension = instance.cachedDimensionType
        if (y < dimension.minY() || y >= dimension.minY() + dimension.height()) return null
        if (!instance.isChunkLoaded(Math.floorDiv(x, 16), Math.floorDiv(z, 16))) return null
        return instance.getBlock(x, y, z)
    }

    private fun blocksMotion(block: Block): Boolean = registries.blocks()
        .getTag(BlockTags.BLOCKS_MOTION)
        ?.contains(RegistryKey.of(block.key())) == true

    private fun <T> matchesEntrySet(
        entries: RegistryEntrySet,
        registry: Registry<T>,
        value: RegistryKey<T>,
    ): Boolean = entries.values.any { reference ->
        if (reference.tag) {
            registry.getTag(reference.key)?.contains(value) == true
        } else {
            reference.key == value.key()
        }
    }
}

private fun <T : Comparable<T>> LocationRange<T>?.matchesOptional(value: T): Boolean =
    this == null || matches(value)

private fun <T : Comparable<T>> LocationRange<T>.matches(value: T): Boolean {
    val minimum = min
    val maximum = max
    return (minimum == null || value >= minimum) && (maximum == null || value <= maximum)
}
