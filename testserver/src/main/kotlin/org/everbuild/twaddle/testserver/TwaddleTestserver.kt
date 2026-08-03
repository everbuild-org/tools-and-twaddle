package org.everbuild.twaddle.testserver

import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import kotlinx.coroutines.awaitCancellation
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.minestom.server.command.builder.Command
import net.minestom.server.command.builder.arguments.Argument
import net.minestom.server.command.builder.arguments.ArgumentLiteral
import net.minestom.server.component.DataComponents
import net.minestom.server.coordinate.Pos
import net.minestom.server.entity.EquipmentSlot
import net.minestom.server.entity.GameMode
import net.minestom.server.entity.Player
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent
import net.minestom.server.event.player.PlayerGameModeRequestEvent
import net.minestom.server.event.player.PlayerSpawnEvent
import net.minestom.server.event.server.ServerListPingEvent
import net.minestom.server.instance.LightingChunk
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.network.player.ResolvableProfile
import net.minestom.server.ping.Status
import net.minestom.server.utils.chunk.ChunkSupplier
import org.everbuild.twaddle.core.TwaddleContext
import org.everbuild.twaddle.core.TwaddleServerContext
import org.everbuild.twaddle.core.logging.field
import org.everbuild.twaddle.core.logging.info
import org.everbuild.twaddle.core.logging.logger
import org.everbuild.twaddle.core.minestom.listen
import org.everbuild.twaddle.core.runTwaddleApplication
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

val logger = logger()

class TestCommand : Command("test") {
    init {
        addSyntax({ sender, _ ->
            if (sender !is Player) return@addSyntax

            sender.openInventory(FurnaceContainer())
        }, ArgumentLiteral("furnace-inventory"))
    }
}

suspend fun main() = runTwaddleApplication { cx ->
    val server = MinecraftServer.init(Auth.Online())

    val instance = MinecraftServer.getInstanceManager().createInstanceContainer()
    instance.setGenerator { unit ->
        unit.modifier().apply {
            fillHeight(0, 64, Block.WHITE_CONCRETE)
            fillHeight(64, 65, Block.LIME_STAINED_GLASS)
        }
    }

    instance.chunkSupplier = { x, y, i -> LightingChunk(x, y, i) }

    MinecraftServer.getCommandManager()
        .register(TestCommand())

    val pngFileBytes = TestCommand::class.java.classLoader.getResourceAsStream("server-icon.png")!!.readAllBytes()

    MinecraftServer.getGlobalEventHandler()
        .listen { event: AsyncPlayerConfigurationEvent ->
            event.spawningInstance = instance
            event.player.respawnPoint = Pos(0.0, 66.0, 0.0)
            event.player.gameMode = GameMode.CREATIVE
            event.player.permissionLevel = 4
        }
        .listen { event: PlayerGameModeRequestEvent ->
            event.player.gameMode = event.requestedGameMode
        }
        .listen { event: ServerListPingEvent ->
            event.status = Status.builder()
                .favicon(pngFileBytes)
                .description(
                    Component.text("Tools and Twaddle", NamedTextColor.RED)
                        .append(Component.newline())
                        .append(
                            Component.text("» Kotlin Testserver", NamedTextColor.GRAY)
                        )
                )
                .build()
        }

    logger.info(
        "auth" field "online",
        "port" field 25565,
    ) {
        "Testserver binding"
    }


    server.start("0.0.0.0", 25565)
    awaitCancellation()
}
