package org.everbuild.twaddle.lifecycle

import net.minestom.server.MinecraftServer

class TwaddleServerContext(
    val server: MinecraftServer,
    context: TwaddleContext,
) : TwaddleContext by context