package org.everbuild.twaddle.core

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer

val twaddleContextTests by testSuite("Twaddle context") {
    test("Ordinary module functions return their final value") {
        val context = TwaddleContext()
        val resource = OwnedTestResource("direct")

        val api = context.installDirect(resource)

        api.resource shouldBe resource
        resource.closed shouldBe false

        context.shutdown()
        resource.closed shouldBe true
    }

    test("Phased module functions return work requiring an initialized server") {
        val context = TwaddleContext()
        val resource = OwnedTestResource("phased")
        val prepared = context.preparePhased(resource)

        resource.owned shouldBe false

        val api = prepared(MinecraftServer())

        api.resource shouldBe resource
        resource.owned shouldBe true
        resource.closed shouldBe false

        context.shutdown()
        resource.closed shouldBe true
    }

    test("Shutdown cancels lifetime work before closing resources") {
        val context = TwaddleContext()
        val events = mutableListOf<String>()
        val started = CompletableDeferred<Unit>()
        context.own(RecordingResource("resource", events))
        context.launch {
            try {
                started.complete(Unit)
                awaitCancellation()
            } finally {
                events += "job"
            }
        }
        started.await()

        context.shutdown()

        events shouldBe listOf("job", "resource")
        context.coroutineContext[Job]?.isActive shouldBe false
    }

    test("Shutdown closes resources in reverse order and is idempotent") {
        val context = TwaddleContext()
        val events = mutableListOf<String>()
        context.own(RecordingResource("first", events))
        context.own(RecordingResource("second", events))

        context.shutdown()
        context.shutdown()

        events shouldBe listOf("second", "first")
    }

    test("A resource offered after shutdown is closed and rejected") {
        val context = TwaddleContext()
        context.shutdown()
        val resource = OwnedTestResource("late")

        val result = runCatching { context.own(resource) }

        (result.exceptionOrNull() is IllegalStateException) shouldBe true
        resource.closed shouldBe true
    }
}

private suspend fun TwaddleContext.installDirect(
    resource: OwnedTestResource,
): TestApi {
    resource.owned = true
    return TestApi(own(resource))
}

private suspend fun TwaddleContext.preparePhased(
    resource: OwnedTestResource,
): AfterInit<TestApi> = {
    resource.owned = true
    TestApi(own(resource))
}

private class TestApi(
    val resource: OwnedTestResource,
)

private open class OwnedTestResource(
    private val name: String,
) : AutoCloseable {
    var owned = false
    var closed = false
        private set

    override fun close() {
        closed = true
    }
}

private class RecordingResource(
    name: String,
    private val events: MutableList<String>,
) : OwnedTestResource(name) {
    override fun close() {
        super.close()
        events += name
    }

    private val name = name
}
