package org.everbuild.twaddle.data_driven

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.event.EventNode
import org.everbuild.twaddle.data_driven.loader.CodecLoader
import org.everbuild.twaddle.data_driven.loading.DataLoader
import org.everbuild.twaddle.data_driven.loading.applyAll
import org.everbuild.twaddle.data_driven.loading.bindDataSource
import org.everbuild.twaddle.data_driven.loading.classpath
import org.everbuild.twaddle.data_driven.loading.folder
import org.everbuild.twaddle.data_driven.loading.invoke
import org.everbuild.twaddle.data_driven.loading.zip
import org.everbuild.twaddle.data_driven.pattern.GlobPattern
import org.everbuild.twaddle.data_driven.pattern.PackFolderPattern
import org.everbuild.twaddle.data_driven.resource.ClasspathSource
import org.everbuild.twaddle.data_driven.resource.DataSource
import org.everbuild.twaddle.data_driven.resource.FolderSource
import org.everbuild.twaddle.data_driven.resource.RawResource
import org.everbuild.twaddle.data_driven.resource.ZipSource
import org.everbuild.twaddle.data_driven.source.DataSourceReloadEvent
import org.everbuild.twaddle.data_driven.source.DataSources
import org.everbuild.twaddle.data_driven.source.ReloadResult
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.net.URLClassLoader
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private data class TestDefinition(val value: String)
private object TestResourceAnchor
private class ChildClasspathAnchor

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
    test("Raw resources compare byte contents and reload results expose both success names") {
        val first = RawResource("entry.txt", "memory:one", byteArrayOf(1, 2, 3))
        val same = RawResource("entry.txt", "memory:one", byteArrayOf(1, 2, 3))
        first.text() shouldBe "\u0001\u0002\u0003"
        first shouldBe same
        first.hashCode() shouldBe same.hashCode()
        (first == first) shouldBe true
        first.equals(null) shouldBe false
        first.equals("entry.txt") shouldBe false
        (first == first.copy(path = "other.txt")) shouldBe false
        (first == first.copy(origin = "memory:two")) shouldBe false
        (first == first.copy(contents = byteArrayOf(3, 2, 1))) shouldBe false

        ReloadResult.success().succeeded shouldBe true
        ReloadResult.failure(emptyList()).succeeded shouldBe false
        ReloadResult.failure(emptyList()).successful shouldBe false
    }

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

    test("Repeated registry DSL blocks accumulate declarations and static keys are validated") {
        val registry = DataDriven<String>()
        val coordinator = DataSources {
            registry via { addStatic("test:first", "first") }
            registry via { +("test:second" to "second") }
        }
        coordinator.reloadBlocking().successful shouldBe true
        registry.snapshot().entries shouldBe mapOf(
            Key.key("test:first") to "first",
            Key.key("test:second") to "second",
        )
        shouldThrow<IllegalArgumentException> {
            DataSources { registry via { addStatic("Invalid Namespace:key", "bad") } }
        }
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
        val nestedCategory = PackFolderPattern("data").resource("worldgen/biome", ".json")
        nestedCategory.key("data/minecraft/worldgen/biome/nether/wastes.json") shouldBe Key.key("minecraft:nether/wastes")
        nestedCategory.key("data/minecraft/worldgen/feature/nether/wastes.json") shouldBe null
    }

    test("Patterns reject malformed configuration and ignore invalid resource paths") {
        shouldThrow<IllegalArgumentException> { GlobPattern("", "test") }
        shouldThrow<IllegalArgumentException> { GlobPattern("/absolute/*.json", "test") }
        shouldThrow<IllegalArgumentException> { GlobPattern("data\\\\*.json", "test") }
        shouldThrow<IllegalArgumentException> { GlobPattern("*.json", " ") }
        shouldThrow<IllegalArgumentException> { GlobPattern("[", "test") }
        val glob = GlobPattern("data/*.json", "test")
        glob.key("") shouldBe null
        glob.key("/data/a.json") shouldBe null
        glob.key("data/../a.json") shouldBe null
        glob.key("data/a\\\\.json") shouldBe null

        shouldThrow<IllegalArgumentException> { PackFolderPattern("") }
        shouldThrow<IllegalArgumentException> { PackFolderPattern("../data") }
        shouldThrow<IllegalArgumentException> { PackFolderPattern("data//assets") }
        val pack = PackFolderPattern().resource("recipes", ".json")
        pack.key("data//test/recipes/item.json") shouldBe null
        pack.key("data/test/recipes/../item.json") shouldBe null
        pack.key("data\\\\test/recipes/item.json") shouldBe null
        shouldThrow<IllegalArgumentException> { PackFolderPattern().resource("", ".json") }
        shouldThrow<IllegalArgumentException> { PackFolderPattern().resource("../recipes", ".json") }
        shouldThrow<IllegalArgumentException> { PackFolderPattern().resource("recipes", "json/path") }
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

    test("Folder source rejects a non-directory root and symlinks escaping the root") {
        val parent = tempDirectory()
        try {
            val fileRoot = parent.write("not-a-directory", "file")
            shouldThrow<IllegalArgumentException> { FolderSource(fileRoot).discover() }

            val root = Files.createDirectories(parent.resolve("root"))
            val outside = parent.write("outside.txt", "outside")
            Files.createSymbolicLink(root.resolve("escape.txt"), outside)
            shouldThrow<IllegalArgumentException> { FolderSource(root).discover() }
        } finally {
            parent.deleteTree()
        }
    }

    test("Classpath sources discover test resources from the anchor classloader") {
        val found = ClasspathSource(TestResourceAnchor::class).discover()
        found.map { it.path } shouldContain "data-driven/fixture.txt"
        found.single { it.path == "data-driven/fixture.txt" }.text() shouldBe "classpath fixture\n"

        val appClasspathResource = "net/minestom/server/codec/Codec.class"
        check(ClassLoader.getSystemResource(appClasspathResource) != null) {
            "Minestom codec class should be visible from the standard application classloader"
        }
        ClasspathSource(String::class).discover().map { it.path } shouldContain appClasspathResource

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

        val layered = tempDirectory()
        try {
            val parentRoot = Files.createDirectories(layered.resolve("parent"))
            val childRoot = Files.createDirectories(layered.resolve("child"))
            parentRoot.write("shadowed.txt", "parent bytes")
            childRoot.write("shadowed.txt", "child bytes")
            val anchorName = ChildClasspathAnchor::class.java.name.replace('.', '/')
            val anchorBytes = ChildClasspathAnchor::class.java.getResourceAsStream("/${anchorName}.class")!!.use { it.readBytes() }
            childRoot.resolve("$anchorName.class").also { Files.createDirectories(it.parent); Files.write(it, anchorBytes) }
            URLClassLoader(arrayOf(parentRoot.toUri().toURL()), ClassLoader.getPlatformClassLoader()).use { parent ->
                URLClassLoader(arrayOf(childRoot.toUri().toURL()), parent).use { child ->
                    val anchor = Class.forName(ChildClasspathAnchor::class.java.name, true, child).kotlin
                    val resource = ClasspathSource(anchor).discover().single { it.path == "shadowed.txt" }
                    resource.text() shouldBe "parent bytes"
                    resource.origin.contains("parent") shouldBe true
                }
            }

            val parentArchive = layered.resolve("parent.jar")
            ZipOutputStream(Files.newOutputStream(parentArchive)).use { zip ->
                zip.putNextEntry(ZipEntry("shadowed.txt"))
                zip.write("parent JAR bytes".toByteArray())
                zip.closeEntry()
            }
            URLClassLoader(arrayOf(parentArchive.toUri().toURL()), ClassLoader.getPlatformClassLoader()).use { parent ->
                URLClassLoader(arrayOf(childRoot.toUri().toURL()), parent).use { child ->
                    val anchor = Class.forName(ChildClasspathAnchor::class.java.name, true, child).kotlin
                    val resource = ClasspathSource(anchor).discover().single { it.path == "shadowed.txt" }
                    resource.text() shouldBe "parent JAR bytes"
                    resource.origin.contains("parent.jar") shouldBe true
                }
            }
        } finally {
            layered.deleteTree()
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

    test("Named and shorthand source bindings support every source kind and preserve composition order") {
        val root = tempDirectory()
        try {
            val folderPath = Files.createDirectories(root.resolve("folder-source"))
            folderPath.write("folder/item.json", "folder value")
            val archive = root.resolve("source.zip")
            ZipOutputStream(Files.newOutputStream(archive)).use { zip ->
                zip.putNextEntry(ZipEntry("archive/item.json"))
                zip.write("archive value".toByteArray())
                zip.closeEntry()
            }

            val memorySources = listOf("first", "second").map { label ->
                memorySource("memory:$label") {
                    listOf(RawResource("$label.txt", "memory:$label", label.toByteArray()))
                }
            }
            val composed = bindDataSource(memorySources[0], GlobPattern("*.txt", "composed"), DataLoader { it.text() }) +
                bindDataSource(memorySources[1], GlobPattern("*.txt", "composed"), DataLoader { it.text() })

            val namedRegistry = DataDriven<String>()
            val shortcutRegistry = DataDriven<String>()
            val classpathPattern = GlobPattern("data-driven/*.txt", "classpath")
            val folderPattern = GlobPattern("folder/*.json", "folder")
            val archivePattern = GlobPattern("archive/*.json", "archive")
            val textLoader = DataLoader { it.text() }
            val configured = DataSources {
                namedRegistry via {
                    +composed
                    addSource(memorySource("memory:named") {
                        listOf(RawResource("named.txt", "memory:named", "named".toByteArray()))
                    }, GlobPattern("*.txt", "named"), textLoader)
                    addClasspath(classpathPattern, TestResourceAnchor::class, textLoader)
                    addFolder(folderPattern, folderPath, textLoader)
                    addZip(archivePattern, archive, textLoader)
                }
                shortcutRegistry via {
                    +classpath(classpathPattern, TestResourceAnchor::class, textLoader)
                    +folder(folderPattern, folderPath, textLoader)
                    +zip(archivePattern, archive, textLoader)
                }
            }
            configured.reloadBlocking().successful shouldBe true
            namedRegistry[Key.key("composed:first")] shouldBe "first"
            namedRegistry[Key.key("composed:second")] shouldBe "second"
            namedRegistry[Key.key("named:named")] shouldBe "named"
            namedRegistry[Key.key("classpath:fixture")] shouldBe "classpath fixture\n"
            namedRegistry[Key.key("folder:item")] shouldBe "folder value"
            namedRegistry[Key.key("archive:item")] shouldBe "archive value"
            shortcutRegistry.snapshot().entries shouldBe mapOf(
                Key.key("classpath:fixture") to "classpath fixture\n",
                Key.key("folder:item") to "folder value",
                Key.key("archive:item") to "archive value",
            )
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

    test("Discovery errors become diagnostics and preserve the previous snapshot") {
        val registry = DataDriven<String>()
        DataSources { registry via { +("test:old" to "old") } }.reloadBlocking().successful shouldBe true
        val oldSnapshot = registry.snapshot()
        val source = memorySource("memory:unavailable") { throw IOException("source unavailable") }
        val result = DataSources {
            registry via { +source(GlobPattern("*.json", "test"), DataLoader { it.text() }) }
        }.reloadBlocking()

        result.successful shouldBe false
        result.diagnostics.single().message.contains("memory:unavailable") shouldBe true
        result.diagnostics.single().cause!!.message!!.contains("memory:unavailable") shouldBe true
        registry.snapshot() shouldBe oldSnapshot
    }

    test("Cancellation from a source or loader propagates and preserves current snapshots") {
        runBlocking {
            for (cancelAtLoader in listOf(false, true)) {
                val registry = DataDriven<String>()
                DataSources { registry via { +("test:current" to "current") } }.reloadBlocking().successful shouldBe true
                val source = memorySource("memory:cancellation") {
                    if (!cancelAtLoader) throw CancellationException("source cancelled")
                    listOf(RawResource("entry.json", "memory:cancellation", "{}".toByteArray()))
                }
                val loader = DataLoader<String> {
                    if (cancelAtLoader) throw CancellationException("loader cancelled")
                    it.text()
                }
                val coordinator = DataSources {
                    registry via { +source(GlobPattern("*.json", "test"), loader) }
                }
                try {
                    coordinator.reload()
                    error("Expected cancellation")
                } catch (_: CancellationException) {
                    registry.snapshot().entries shouldBe mapOf(Key.key("test:current") to "current")
                    registry.snapshot().revision shouldBe 1L
                }
            }
        }
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

        val listenerFailureNode = EventNode.all("data-driven-failing-listener-test")
        listenerFailureNode.addListener(DataSourceReloadEvent::class.java) { error("listener failed") }
        val published = DataDriven<String>()
        val listenerFailure = DataSources(listenerFailureNode) {
            published via { +("test:published" to "published") }
        }.reloadBlocking()
        listenerFailure.successful shouldBe true
        listenerFailure.diagnostics.single().message shouldBe "Snapshot published, but a reload event listener failed"
        published[Key.key("test:published")] shouldBe "published"
    }

    test("CodecLoader uses Minestom JSON codec diagnostics") {
        val resource = RawResource("text.json", "memory:text", "\"hello\"".toByteArray())
        CodecLoader(Codec.STRING).load(resource) shouldBe "hello"
        shouldThrow<IllegalArgumentException> {
            CodecLoader(Codec.INT).load(resource)
        }.message!!.contains("memory:text") shouldBe true
        val malformedJson = RawResource("broken.json", "memory:broken", "{".toByteArray())
        shouldThrow<IllegalArgumentException> { CodecLoader(Codec.STRING).load(malformedJson) }
            .message!!.contains("memory:broken") shouldBe true
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
