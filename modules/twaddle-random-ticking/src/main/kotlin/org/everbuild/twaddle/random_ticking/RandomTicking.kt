package org.everbuild.twaddle.random_ticking

import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import net.minestom.server.coordinate.BlockVec
import net.minestom.server.event.EventFilter
import net.minestom.server.event.EventNode
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceChunkUnloadEvent
import net.minestom.server.event.instance.InstanceRegisterEvent
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.event.instance.InstanceUnregisterEvent
import net.minestom.server.event.trait.InstanceEvent
import net.minestom.server.instance.Chunk
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.common.block.tick.RandomTickHandler
import org.everbuild.twaddle.common.block.util.withReadLock

private fun Chunk.around(): List<Pair<Int, Int>> = buildList {
    for (dx in -1..1) {
        for (dz in -1..1) {
            if (dx != 0 || dz != 0) {
                add((chunkX + dx) to (chunkZ + dz))
            }
        }
    }
}

private fun innerChunks(instance: Instance): Set<Chunk> {
    val chunks = instance.chunks.toList()
    val coordinates = chunks.map { it.chunkX to it.chunkZ }.toSet()
    return chunks.filter { chunk -> chunk.around().all { it in coordinates } }.toSet()
}

/**
 * Samples blocks in each section of chunks surrounded by eight loaded neighbors.
 * [RANDOM_TICK_SPEED] supplies the number of samples per section, per instance tick.
 * Attach the returned node to the server or instance event handler to enable ticking.
 */
fun getRandomTickingEventNode(): EventNode<InstanceEvent> {
    val random = Random.Default
    val innerChunkCache = ConcurrentHashMap<Instance, Set<Chunk>>()

    return EventNode.type("random-ticking", EventFilter.INSTANCE)
        .addListener(InstanceChunkLoadEvent::class.java) { event ->
            innerChunkCache.remove(event.instance)
        }
        .addListener(InstanceChunkUnloadEvent::class.java) { event ->
            innerChunkCache.remove(event.instance)
        }
        .addListener(InstanceRegisterEvent::class.java) { event ->
            innerChunkCache.remove(event.instance)
        }
        .addListener(InstanceUnregisterEvent::class.java) { event ->
            innerChunkCache.remove(event.instance)
        }
        .addListener(InstanceTickEvent::class.java) { event ->
            val speed = event.instance.getTag(RANDOM_TICK_SPEED).coerceAtLeast(0)
            for (chunk in innerChunkCache.computeIfAbsent(event.instance, ::innerChunks)) {
                val minChunkX = chunk.chunkX * 16
                val minChunkZ = chunk.chunkZ * 16
                for (index in chunk.sections.indices) {
                    val minSectionPos = (chunk.minSection + index) * 16

                    repeat(speed) {
                        val randomValue = random.nextLong()
                        val xRandom = (randomValue and 0xF).toInt()
                        val yRandom = (randomValue shr 4 and 0xF).toInt()
                        val zRandom = (randomValue shr 8 and 0xF).toInt()
                        val block = chunk.withReadLock {
                            chunk.getBlock(
                                xRandom,
                                yRandom + minSectionPos,
                                zRandom,
                                Block.Getter.Condition.CACHED,
                            )
                        } ?: return@repeat
                        val handler = block.handler() ?: return@repeat
                        if (handler !is RandomTickHandler || !handler.shouldRandomTick(block)) return@repeat
                        val pos = BlockVec(xRandom + minChunkX, yRandom + minSectionPos, zRandom + minChunkZ)
                        val newBlock = handler.onRandomTick(
                            RandomTickHandler.RandomTick(block, event.instance, pos)
                        ) ?: return@repeat
                        if (newBlock == block) return@repeat
                        event.instance.setBlock(pos, newBlock)
                    }
                }
            }
        }
}
