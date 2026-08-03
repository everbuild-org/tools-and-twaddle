package org.everbuild.twaddle.core

import net.minestom.server.MinecraftServer

class TwaddleServerContext(
    val server: MinecraftServer,
    context: TwaddleContext,
) : TwaddleContext by context