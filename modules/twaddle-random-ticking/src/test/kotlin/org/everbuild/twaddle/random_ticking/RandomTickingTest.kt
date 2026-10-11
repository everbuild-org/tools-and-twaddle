package org.everbuild.twaddle.random_ticking

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.nulls.shouldNotBeNull
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import net.kyori.adventure.key.Key
import net.minestom.server.MinecraftServer
import net.minestom.server.event.instance.InstanceChunkLoadEvent
import net.minestom.server.event.instance.InstanceTickEvent
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockHandler
import org.everbuild.twaddle.common.block.tick.RandomTickHandler
import org.everbuild.twaddle.core.test.envTest

val RandomTickingTests by testSuite {
    envTest("shouldRandomTick filters sampled blocks before invoking their handler") { env ->
        val instance = env.createEmptyInstance()
        val ticks = mutableListOf<RandomTickHandler.RandomTick>()
        val handler = object : BlockHandler, RandomTickHandler {
            override fun getKey(): Key = Key.key("test:conditional_random_tick")
            override fun shouldRandomTick(block: Block): Boolean = block.key() == Block.STONE.key()
            override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block {
                ticks += randomTick
                return Block.GOLD_BLOCK
            }
        }
        val allowedBlock = Block.STONE.withHandler(handler)
        val rejectedBlock = Block.DIRT.withHandler(handler)
        instance.setGenerator {
            it.modifier().fillHeight(-64, -48, allowedBlock)
            it.modifier().fillHeight(-48, -32, rejectedBlock)
        }
        instance.randomTickSpeed = 1
        for (x in -1..1) {
            for (z in -1..1) instance.loadChunk(x, z).join()
        }

        getRandomTickingEventNode().call(InstanceTickEvent(instance, 50, 0))

        ticks.size shouldBe 1
        val tick = ticks.single()
        tick.block shouldBe allowedBlock
        (tick.blockPosition.blockY() in -64 until -48) shouldBe true
        instance.getBlock(tick.blockPosition) shouldBe Block.GOLD_BLOCK
        for (x in 0 until 16) {
            for (y in -48 until -32) {
                for (z in 0 until 16) {
                    instance.getBlock(x, y, z) shouldBe rejectedBlock
                }
            }
        }
    }

    envTest("Loaded neighbours enable ticking and chunk changes invalidate the cached selection") { env ->
        val instance = env.createEmptyInstance()
        val ticks = mutableListOf<RandomTickHandler.RandomTick>()
        val handler = object : BlockHandler, RandomTickHandler {
            override fun getKey(): Key = Key.key("test:random_tick")
            override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block {
                ticks += randomTick
                return Block.DIRT.withHandler(this)
            }
        }
        instance.setGenerator { it.modifier().fillHeight(-64, -48, Block.STONE.withHandler(handler)) }
        instance.randomTickSpeed = 2
        val node = getRandomTickingEventNode()
        MinecraftServer.getGlobalEventHandler().addChild(node)

        val centerX = -2
        val centerZ = -3
        val neighbourLoads = LinkedBlockingQueue<InstanceChunkLoadEvent>()
        node.addListener(InstanceChunkLoadEvent::class.java) { event ->
            if (event.instance === instance &&
                event.chunk.chunkX == centerX + 1 && event.chunk.chunkZ == centerZ + 1
            ) {
                neighbourLoads.add(event)
            }
        }
        fun loadMissingNeighbour() {
            instance.loadChunk(centerX + 1, centerZ + 1).join()
            // Minestom completes the load future before dispatching the cache-invalidating event.
            neighbourLoads.poll(5, TimeUnit.SECONDS).shouldNotBeNull()
        }

        for (dx in -1..1) {
            for (dz in -1..1) {
                if (dx != 1 || dz != 1) instance.loadChunk(centerX + dx, centerZ + dz).join()
            }
        }
        node.call(InstanceTickEvent(instance, 50, 0))
        ticks.size shouldBe 0

        loadMissingNeighbour()
        node.call(InstanceTickEvent(instance, 100, 50))
        ticks.size shouldBe 2
        ticks.forEach { tick ->
            tick.instance shouldBe instance
            tick.blockPosition.chunkX() shouldBe centerX
            tick.blockPosition.chunkZ() shouldBe centerZ
            (tick.blockPosition.blockY() in -64 until -48) shouldBe true
            instance.getBlock(tick.blockPosition) shouldBe Block.DIRT.withHandler(handler)
        }

        instance.randomTickSpeed = 0
        node.call(InstanceTickEvent(instance, 150, 100))
        ticks.size shouldBe 2
        instance.randomTickSpeed = 4
        node.call(InstanceTickEvent(instance, 200, 150))
        ticks.size shouldBe 6

        instance.unloadChunk(centerX + 1, centerZ + 1)
        node.call(InstanceTickEvent(instance, 250, 200))
        ticks.size shouldBe 6
        loadMissingNeighbour()
        node.call(InstanceTickEvent(instance, 300, 250))
        ticks.size shouldBe 10
    }

    envTest("Default speed samples three times per section and null results leave blocks unchanged") { env ->
        val instance = env.createEmptyInstance()
        val ticks = mutableListOf<RandomTickHandler.RandomTick>()
        val handler = object : BlockHandler, RandomTickHandler {
            override fun getKey(): Key = Key.key("test:no_change")
            override fun onRandomTick(randomTick: RandomTickHandler.RandomTick): Block? {
                ticks += randomTick
                return null
            }
        }
        val ordinaryHandler = BlockHandler { Key.key("test:ordinary") }
        val tickingBlock = Block.STONE.withHandler(handler)
        instance.setGenerator {
            it.modifier().fillHeight(-64, -48, tickingBlock)
            it.modifier().fillHeight(-48, -32, Block.STONE.withHandler(ordinaryHandler))
            it.modifier().fillHeight(-32, -16, Block.STONE)
        }
        val node = getRandomTickingEventNode()
        for (x in -1..1) {
            for (z in -1..1) instance.loadChunk(x, z).join()
        }
        node.call(InstanceTickEvent(instance, 50, 0))
        ticks.size shouldBe 3
        ticks.forEach { tick ->
            tick.block shouldBe tickingBlock
            instance.getBlock(tick.blockPosition) shouldBe tickingBlock
        }
        node.call(InstanceTickEvent(instance, 100, 50))
        ticks.size shouldBe 6
    }
}
