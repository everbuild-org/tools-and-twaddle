package org.everbuild.twaddle.core

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import net.minestom.server.MinecraftServer

val twaddleContextTests by testSuite("Twaddle context") {
    test("Ordinary module functions return their final value") {
        val context = TwaddleContextImpl()
        val resource = OwnedTestResource("direct")

        val api = context.installDirect(resource)

        api.resource shouldBe resource
        resource.closed shouldBe false

        context.shutdown()
        resource.closed shouldBe true
    }

    test("Phased module functions return work requiring an initialized server") {
        val context = TwaddleContextImpl()
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
        val context = TwaddleContextImpl()
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
        val context = TwaddleContextImpl()
        val events = mutableListOf<String>()
        context.own(RecordingResource("first", events))
        context.own(RecordingResource("second", events))

        context.shutdown()
        context.shutdown()

        events shouldBe listOf("second", "first")
    }

    test("A resource offered after shutdown is closed and rejected") {
        val context = TwaddleContextImpl()
        context.shutdown()
        val resource = OwnedTestResource("late")

        val result = runCatching { context.own(resource) }

        (result.exceptionOrNull() is IllegalStateException) shouldBe true
        resource.closed shouldBe true
    }

    test("Forks are named child contexts owned by their parent") {
        val events = mutableListOf<String>()
        val parent = TwaddleContextImpl()
        parent.own(RecordingResource("parent-first", events))
        val child = parent.fork("inventory")
        child.own(RecordingResource("child", events))
        parent.own(RecordingResource("parent-last", events))

        child.coroutineContext[CoroutineName]?.name shouldBe "twaddle/inventory"

        parent.shutdown()

        events shouldBe listOf("parent-last", "child", "parent-first")
        child.coroutineContext[Job]?.isActive shouldBe false
    }

    test("A fork can shut down without shutting down its parent") {
        val parent = TwaddleContextImpl()
        val child = parent.fork("temporary")

        child.shutdown()

        parent.coroutineContext[Job]?.isActive shouldBe true
        child.coroutineContext[Job]?.isActive shouldBe false

        parent.shutdown()
    }

    test("A context is cancelled with its coroutine parent") {
        val parentJob = Job()
        val context = TwaddleContextImpl(parentJob)

        parentJob.cancel()
        context.coroutineContext[Job]?.join()

        context.coroutineContext[Job]?.isActive shouldBe false
        context.shutdown()
    }

    test("Nested fork names describe their complete ownership path") {
        val parent = TwaddleContextImpl()
        val child = parent.fork("inventory")
        val grandchild = child.fork("persistence")

        grandchild.coroutineContext[CoroutineName]?.name shouldBe "twaddle/inventory/persistence"

        parent.shutdown()
    }

    test("A fork cannot be created after parent shutdown") {
        val parent = TwaddleContextImpl()
        parent.shutdown()

        val result = runCatching { parent.fork("late") }

        (result.exceptionOrNull() is IllegalStateException) shouldBe true
    }

    test("An initialized context enriches the same lifetime with its server") {
        val context: TwaddleContext = TwaddleContextImpl()
        val server = MinecraftServer()
        val initialized = context.initialized(server)
        val resource = OwnedTestResource("initialized")

        initialized.server shouldBe server
        initialized.own(resource)
        initialized.shutdown()

        resource.closed shouldBe true
        context.coroutineContext[Job]?.isActive shouldBe false
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
