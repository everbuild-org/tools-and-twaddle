package org.everbuild.twaddle.testserver

import net.minestom.server.Auth
import net.minestom.server.MinecraftServer
import kotlinx.coroutines.awaitCancellation
import org.everbuild.twaddle.core.TwaddleContext
import org.everbuild.twaddle.core.logging.field
import org.everbuild.twaddle.core.logging.info
import org.everbuild.twaddle.core.logging.logger
import org.everbuild.twaddle.core.runTwaddleApplication

val logger = logger()

suspend fun main() = runTwaddleApplication { _ ->
    val server = MinecraftServer.init(Auth.Online())

    logger.info(
        "auth" field "online",
        "port" field 25565,
    ) {
        "Testserver binding"
    }

    server.start("0.0.0.0", 25565)
}
