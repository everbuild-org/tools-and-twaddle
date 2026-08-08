package net.minestom.testing

import net.minestom.server.ServerProcess

internal fun newEnvImpl(process: ServerProcess): Env = EnvImpl(process)

internal fun Env.cleanup() {
    (this as EnvImpl).cleanup()
}