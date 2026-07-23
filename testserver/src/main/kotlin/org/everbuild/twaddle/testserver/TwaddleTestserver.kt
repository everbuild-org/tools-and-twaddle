package org.everbuild.twaddle.testserver

import net.minestom.server.Auth
import org.everbuild.twaddle.core.logging.field
import org.everbuild.twaddle.core.logging.info
import org.everbuild.twaddle.core.logging.logger
import org.everbuild.twaddle.core.runFoundry

val logger = logger()

suspend fun main() = runFoundry {
    complete {
        logger.info(
            "auth" field "online",
            "port" field 25565,
        ) {
            "Testserver binding"
        }
        it
            .withAuth(Auth.Online())
            .bind(25565)
    }
}
