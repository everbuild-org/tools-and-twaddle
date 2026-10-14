package org.everbuild.twaddle.data_driven

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.event.EventNode
import org.everbuild.twaddle.data_driven.loader.CodecLoader
import org.everbuild.twaddle.data_driven.loading.DataLoader
import org.everbuild.twaddle.data_driven.loading.applyAll
import org.everbuild.twaddle.data_driven.loading.invoke
import org.everbuild.twaddle.data_driven.pattern.GlobPattern
import org.everbuild.twaddle.data_driven.pattern.PackFolderPattern
import org.everbuild.twaddle.data_driven.resource.ClasspathSource
import org.everbuild.twaddle.data_driven.resource.DataSource
import org.everbuild.twaddle.data_driven.resource.FolderSource
import org.everbuild.twaddle.data_driven.resource.RawResource
import org.everbuild.twaddle.data_driven.resource.ZipSource
import org.everbuild.twaddle.data_driven.source.DataSourceReloadEvent
import org.everbuild.twaddle.data_driven.source.DataSources
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private data class TestDefinition(val value: String)
private object TestResourceAnchor

private val stringLoader = DataLoader { resource -> TestDefinition(resource.text()) }
private val testKey = Key.key("test:entry")
private fun tempDirectory(): Path = Files.createTempDirectory("twaddle-data-driven")
private fun Path.write(relative: String, content: String): Path = resolve(relative).also {
    Files.createDirectories(it.parent)
    Files.writeString(it, content)
}

private fun Path.deleteTree() {
    if (!Files.exists(this)) return
    Files.walk(this).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::deleteIfExists) }
}

private fun memorySource(description: String, discover: () -> List<RawResource>): DataSource = object : DataSource {
    override val description: String = description
    override fun discover(): List<RawResource> = discover()
}

@Suppress("RedundantSamConstructor")
val DataDrivenTests by testSuite {
    test("Registries publish immutable replacements and retain old snapshots") {
        val registry = DataDriven<String>()
        registry.snapshot().entries shouldBe emptyMap()
        registry.snapshot().revision shouldBe 0L
        val coordinator = DataSources { registry via { +("test:one" to "first") } }
        coordinator.reloadBlocking().successful shouldBe true
        val old = registry.snapshot()
        registry[Key.key("test:one")] shouldBe "first"
        DataSources { registry via { +("test:two" to "second") } }.reloadBlocking().successful shouldBe true
        registry.snapshot().entries.keys.toList() shouldBe listOf(Key.key("test:two"))
        registry.snapshot().revision shouldBe 2L
        old.entries shouldBe mapOf(Key.key("test:one") to "first")
        shouldThrow<UnsupportedOperationException> { (old.entries as MutableMap).clear() }

        val independent = DataDriven<String>()
        independent[testKey] shouldBe null
        DataSources { registry via { +("test:three" to "three") }; independent via { +("test:entry" to "independent") } }
            .reloadBlocking().successful shouldBe true
        independent[testKey] shouldBe "independent"
    }

    test("Glob and pack folder patterns map matching paths to validated namespaced keys") {
        val glob = GlobPattern("data/loot_tables/**/*.json", "asorda")
        glob.key("data/loot_tables/example.json") shouldBe Key.key("asorda:example")
        glob.key("data/loot_tables/nested/rare.json") shouldBe Key.key("asorda:nested/rare")
        glob.key("other/nested/rare.json") shouldBe null
        GlobPattern("data/*.json", "Bad Namespace").key("data/a.json") shouldBe null
        GlobPattern("*.txt", "test").key("entry.txt") shouldBe testKey

        val pattern = PackFolderPattern("data").resource("recipes", ".json")
        pattern.key("data/asorda/recipes/example.json") shouldBe Key.key("asorda:example")
        pattern.key("data/asorda/recipes/smelting/steel.json") shouldBe Key.key("asorda:smelting/steel")
        pattern.key("data/minecraft/recipes/crafting/test.json") shouldBe Key.key("minecraft:crafting/test")
        pattern.key("data/asorda/loot_tables/no.json") shouldBe null
        pattern.key("data/Bad Namespace/recipes/a.json") shouldBe null
    }

    test("Folder and ZIP sources read normalized nested paths and report missing roots as empty") {
        val root = tempDirectory()
        try {
            root.write("nested/item.json", "folder")
            FolderSource(root).discover().single().let {
                it.path shouldBe "nested/item.json"
                it.text() shouldBe "folder"
                it.origin.contains("nested/item.json") shouldBe true
            }
            FolderSource(root.resolve("missing")).discover() shouldBe emptyList()

            val archive = root.resolve("data.zip")
            ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                zip.putNextEntry(ZipEntry("nested/inside.json"))
                zip.write("zip".toByteArray())
                zip.closeEntry()
            }
            val zipResources = ZipSource(archive).discover()
            zipResources.map { it.path } shouldBe listOf("nested/inside.json")
            zipResources.single().text() shouldBe "zip"
            ZipSource(root.resolve("missing.zip")).discover() shouldBe emptyList()
            val badArchive = root.resolve("bad.zip")
            ZipOutputStream(Files.newOutputStream(badArchive)).use { zip ->
                zip.putNextEntry(ZipEntry("../escape"))
                zip.write("bad".toByteArray())
                zip.closeEntry()
            }
            shouldThrow<IOException> { ZipSource(badArchive).discover() }
        } finally {
            root.deleteTree()
        }
    }

    test("Classpath sources discover test resources from the anchor classloader") {
        val found = ClasspathSource(TestResourceAnchor::class).discover()
        found.map { it.path } shouldContain "data-driven/fixture.txt"
        found.single { it.path == "data-driven/fixture.txt" }.text() shouldBe "classpath fixture\n"

        val temp = tempDirectory()
        try {
            val archive = temp.resolve("plugin.jar")
            val anchorName = TestResourceAnchor::class.java.name.replace('.', '/')
            val anchorBytes =
                TestResourceAnchor::class.java.getResourceAsStream("/${anchorName}.class")!!.use { it.readBytes() }
            ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                zip.putNextEntry(ZipEntry("$anchorName.class"))
                zip.write(anchorBytes)
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("data-driven/jar-fixture.txt"))
                zip.write("jar fixture".toByteArray())
                zip.closeEntry()
            }
            URLClassLoader(arrayOf(archive.toUri().toURL()), ClassLoader.getPlatformClassLoader()).use { loader ->
                val anchor = Class.forName(TestResourceAnchor::class.java.name, true, loader).kotlin
                val jarResource = ClasspathSource(anchor).discover().single { it.path == "data-driven/jar-fixture.txt" }
                jarResource.text() shouldBe "jar fixture"
            }
        } finally {
            temp.deleteTree()
        }
    }

    test("Reload replaces removed resources, preserves static definitions, and applies later overrides") {
        val root = tempDirectory()
        try {
            val file = root.write("data/test/test/entry.json", "initial")
            val registry = DataDriven<TestDefinition>()
            val source = FolderSource(root)
            val pattern = PackFolderPattern().resource("test", ".json")
            val sources = DataSources {
                registry via {
                    +("test:static" to TestDefinition("static"))
                    +source(pattern, stringLoader)
                }
            }
            sources.reloadBlocking().successful shouldBe true
            registry[testKey]!!.value shouldBe "initial"
            Files.writeString(file, "updated")
            sources.reloadBlocking().successful shouldBe true
            registry[testKey]!!.value shouldBe "updated"
            Files.delete(file)
            sources.reloadBlocking().successful shouldBe true
            registry[testKey] shouldBe null
            registry[Key.key("test:static")]!!.value shouldBe "static"

            root.write("data/test/test/entry.json", "earlier")
            val later = memorySource("memory:later") {
                listOf(
                    RawResource(
                        "data/test/test/entry.json",
                        "memory:later",
                        "later".toByteArray()
                    )
                )
            }
            val overridden = DataSources {
                registry via {
                    +source(pattern, stringLoader)
                    +later(pattern, stringLoader)
                }
            }
            val result = overridden.reloadBlocking()
            result.successful shouldBe true
            result.diagnostics.any { it.key == testKey && it.message.contains("overrides") } shouldBe true
            registry[testKey]!!.value shouldBe "later"
        } finally {
            root.deleteTree()
        }
    }

    test("Decode errors preserve every previous registry snapshot and flow state") {
        val registry = DataDriven<String>()
        val alsoRegistry = DataDriven<String>()
        DataSources {
            registry via { +("test:old" to "old") }
            alsoRegistry via { +("test:also-old" to "also old") }
        }.reloadBlocking().successful shouldBe true
        val oldSnapshot = registry.snapshot()
        val oldAlsoSnapshot = alsoRegistry.snapshot()
        val failingSource = memorySource("memory:broken") {
            listOf(RawResource("broken.json", "memory:broken", "x".toByteArray()))
        }
        val failing = DataSources {
            registry via { +("test:new" to "new") }
            alsoRegistry via {
                +failingSource(
                    GlobPattern("*.json", "test"),
                    DataLoader<String> { error("bad decode") })
            }
        }
        val failed = failing.reloadBlocking()
        failed.successful shouldBe false
        registry.snapshot() shouldBe oldSnapshot
        alsoRegistry.snapshot() shouldBe oldAlsoSnapshot
        registry.snapshots.first() shouldBe oldSnapshot
    }

    test("Multiple registries are prepared before publication and concurrent reloads serialize") {
        val first = DataDriven<String>()
        val second = DataDriven<String>()
        var reads = 0
        val source = memorySource("memory:counter") {
            val current = ++reads
            Thread.sleep(15)
            listOf(RawResource("one.json", "memory", current.toString().toByteArray()))
        }
        val coordinator = DataSources {
            first via { +source(GlobPattern("*.json", "test"), DataLoader { it.text() }) }
            second via { +("test:static" to "ready") }
        }
        val one = async { coordinator.reload() }
        val two = async { coordinator.reload() }
        one.await().successful shouldBe true
        two.await().successful shouldBe true
        reads shouldBe 2
        first.snapshot().revision shouldBe 2L
        second.snapshot().revision shouldBe 2L
    }

    test("StateFlow publishes latest snapshot, event receives exact snapshot, and async API completes") {
        val registry = DataDriven<String>()
        val node = EventNode.all("data-driven-test")
        var observed: DataSourceReloadEvent? = null
        node.addListener(DataSourceReloadEvent::class.java) { observed = it }
        val coordinator = DataSources(node) { registry via { +("test:entry" to "loaded") } }
        registry.snapshots.first().revision shouldBe 0L
        val result = coordinator.reloadAsync().toCompletableFuture().get(5, TimeUnit.SECONDS)
        result.successful shouldBe true
        val snapshot = registry.snapshots.first { it.revision == 1L }
        observed!!.registry shouldBe registry
        (observed.snapshot === snapshot) shouldBe true
        registry.snapshot().entries[testKey] shouldBe "loaded"
    }

    test("CodecLoader uses Minestom JSON codec diagnostics") {
        val resource = RawResource("text.json", "memory:text", "\"hello\"".toByteArray())
        CodecLoader(Codec.STRING).load(resource) shouldBe "hello"
        shouldThrow<IllegalArgumentException> {
            CodecLoader(Codec.INT).load(resource)
        }.message!!.contains("memory:text") shouldBe true
    }

    test("applyAll preserves source order and matches an explicit source loop") {
        val registry = DataDriven<String>()
        val pattern = GlobPattern("*.txt", "test")
        val sources = listOf("first", "second").map { value ->
            memorySource("memory:$value") { listOf(RawResource("entry.txt", "memory:$value", value.toByteArray())) }
        }
        DataSources {
            registry via {
                +sources.applyAll(
                    pattern,
                    DataLoader { it.text() })
            }
        }.reloadBlocking().successful shouldBe true
        registry[testKey] shouldBe "second"
        val loopRegistry = DataDriven<String>()
        DataSources {
            loopRegistry via {
                for (source in sources) +source(pattern, DataLoader { it.text() })
            }
        }.reloadBlocking().successful shouldBe true
        loopRegistry.snapshot().entries shouldBe registry.snapshot().entries
    }

    test("Constructing source bindings does not access data until reload") {
        val registry = DataDriven<String>()
        var discoveries = 0
        val source = memorySource("memory:deferred") {
            discoveries++
            listOf(RawResource("entry.txt", "memory:deferred", "value".toByteArray()))
        }
        val coordinator =
            DataSources { registry via { +source(GlobPattern("*.txt", "test"), DataLoader { it.text() }) } }
        discoveries shouldBe 0
        coordinator.reloadBlocking().successful shouldBe true
        discoveries shouldBe 1
    }
}
