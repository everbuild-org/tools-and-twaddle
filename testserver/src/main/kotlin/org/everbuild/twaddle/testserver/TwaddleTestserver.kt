package org.everbuild.twaddle.testserver

import net.minestom.server.Auth
import org.everbuild.twaddle.core.runFoundry

suspend fun main() = runFoundry {
    complete {
        it
            .withAuth(Auth.Online())
            .bind(25565)
    }
}
