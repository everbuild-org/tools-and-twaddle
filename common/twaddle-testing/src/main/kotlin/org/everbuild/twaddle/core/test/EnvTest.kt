package org.everbuild.twaddle.core.test

import de.infix.testBalloon.framework.core.Test
import de.infix.testBalloon.framework.core.TestConfig
import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.shared.TestElementName
import de.infix.testBalloon.framework.shared.TestRegistering
import net.minestom.server.MinecraftServer
import net.minestom.testing.Env
import net.minestom.testing.cleanup
import net.minestom.testing.newEnvImpl

@TestRegistering
fun TestSuiteScope.envTest(
    @TestElementName name: String,
    testConfig: TestConfig = TestConfig,
    action: suspend Test.ExecutionScope.(Env) -> Unit
) {
    testFixture {
        System.setProperty("minestom.viewable-packet", "false")
        System.setProperty("minestom.inside-test", "true")
        @Suppress("UnstableApiUsage")
        val process = MinecraftServer.updateProcess()
            newEnvImpl(process)
    } closeWith {
        this.cleanup()
    } asParameterForEach {
        test(name, testConfig) { env ->
            action(env)
        }
    }
}