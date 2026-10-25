package org.everbuild.twaddle.testserver

import net.kyori.adventure.key.Key
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.ItemEntity
import net.minestom.server.event.player.PlayerBlockBreakEvent
import org.everbuild.twaddle.data_driven.DataDriven
import org.everbuild.twaddle.data_driven.loader.CodecLoader
import org.everbuild.twaddle.data_driven.loading.invoke
import org.everbuild.twaddle.data_driven.pattern.PackFolderPattern
import org.everbuild.twaddle.data_driven.pattern.ResourcePattern
import org.everbuild.twaddle.data_driven.resource.ClasspathSource
import org.everbuild.twaddle.data_driven.source.DataSources
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DefaultRandomSequenceController
import org.everbuild.twaddle.loot.LootTable
import org.everbuild.twaddle.loot.LootTableResolver
import org.everbuild.twaddle.loot.VanillaLootSupport
import org.everbuild.twaddle.loot.blockBreak
import org.everbuild.twaddle.loot.withLootTableResolver
import org.everbuild.twaddle.structured_logging.field
import org.everbuild.twaddle.structured_logging.info

internal fun blockLootTablePattern(): ResourcePattern {
    val lootTablePattern = PackFolderPattern().resource("loot_table", ".json")
    return object : ResourcePattern {
        override fun key(path: String): Key? = lootTablePattern.key(path)
            ?.takeIf { it.value().startsWith("blocks/") }
    }
}

internal class BlockLootTables private constructor(
    private val tables: DataDriven<LootTable>,
    private val loot: VanillaLootSupport,
) {
    private val randomSequences = DefaultRandomSequenceController()

    fun dropFor(event: PlayerBlockBreakEvent) {
        if (event.isCancelled || event.player.gameMode == GameMode.CREATIVE || event.player.gameMode == GameMode.SPECTATOR) return

        val blockKey = event.block.key()
        val tableKey = Key.key(blockKey.namespace(), "blocks/${blockKey.value()}")
        val table = tables[tableKey] ?: return
        val position = event.blockPosition

        val context = DataContext.blockBreak(event, loot, randomSequences = randomSequences)
            .withLootTableResolver(LootTableResolver(tables::get))

        loot.evaluator.generate(table, context).forEach { item ->
            ItemEntity(item).setInstance(
                event.instance,
                Pos(position.x() + 0.5, position.y() + 0.5, position.z() + 0.5),
            )
        }
    }

    companion object {
        fun load(): BlockLootTables {
            val loot = VanillaLootSupport.vanilla()
            val tables = DataDriven<LootTable>()
            val reload = DataSources {
                tables via {
                    +ClasspathSource(BlockLootTables::class)(
                        blockLootTablePattern(),
                        CodecLoader(loot.codec),
                    )
                }
            }.reloadBlocking()
            check(reload.successful) {
                reload.diagnostics.joinToString("\n") { diagnostic ->
                    listOfNotNull(diagnostic.key?.toString(), diagnostic.origin, diagnostic.message).joinToString(": ")
                }
            }
            check(tables.snapshot().entries.isNotEmpty()) { "No Minecraft block loot tables were loaded" }
            logger.info("blockLootTables" field tables.snapshot().entries.size) { "Loaded block loot tables" }

            return BlockLootTables(tables, loot)
        }
    }
}
