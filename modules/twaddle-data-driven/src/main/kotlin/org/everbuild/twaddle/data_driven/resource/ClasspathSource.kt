package org.everbuild.twaddle.data_driven.resource

import java.io.IOException
import java.net.JarURLConnection
import java.net.URI
import java.net.URL
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.util.jar.JarFile
import kotlin.reflect.KClass

/** Uses [anchor]'s classloader and treats resource paths as rooted at the classpath root. */
class ClasspathSource(anchor: KClass<*>) : DataSource {
    private val anchorClass = anchor.java
    private val classLoader = anchorClass.classLoader ?: ClassLoader.getSystemClassLoader()
    override val description: String = "classpath:${anchorClass.name}"

    override fun discover(): List<RawResource> {
        val roots = linkedMapOf<String, URL>()
        val loader = classLoader
        if (loader is URLClassLoader) {
            loader.urLs.forEach { addRoot(roots, it) }
        }
        // Root enumeration is only a fallback for non-URL classloaders. JAR scanning below handles
        // packaged resources directly; classloader visibility can still hide non-enumerable roots.
        try {
            loader.getResources("").asSequence().forEach { addRoot(roots, it) }
        } catch (failure: IOException) {
            throw IOException("Could not enumerate classpath roots visible to $description", failure)
        }

        val resources = mutableListOf<RawResource>()
        val limitations = mutableListOf<String>()
        for (url in roots.values) {
            when (url.protocol) {
                "file" -> {
                    val root = try { Path.of(url.toURI()) } catch (_: Exception) { continue }
                    if (!Files.isDirectory(root)) continue
                    FolderSource(root).discover().forEach { found ->
                        val visible = loader.getResource(found.path) ?: return@forEach
                        if (visible.protocol == "file") resources += found.copy(origin = "$description:${visible}")
                    }
                }
                "jar" -> {
                    val connection = url.openConnection() as? JarURLConnection ?: continue
                    val jarUri = try { connection.jarFileURL.toURI() } catch (_: Exception) { continue }
                    val jarPath = try { Path.of(jarUri) } catch (_: Exception) { continue }
                    JarFile(jarPath.toFile()).use { jar ->
                        jar.entries().asSequence().filterNot { it.isDirectory }.forEach { entry ->
                            val path = normalizeResourcePath(entry.name) ?: return@forEach
                            // Honor the selected loader's visibility instead of exposing all entries
                            // from a JAR which happens to be adjacent to another visible root.
                            val visible = loader.getResource(path) ?: return@forEach
                            if (visible.protocol != "jar" || !sameJar(visible, jarPath)) return@forEach
                            val content = jar.getInputStream(entry).use { it.readBytes() }
                            resources += RawResource(path, "$description:$visible", content)
                        }
                    }
                }
                else -> limitations += "Cannot enumerate classpath root '$url' (protocol ${url.protocol})"
            }
        }
        if (roots.isEmpty()) limitations += "The classloader exposes no enumerable classpath roots"
        if (resources.isEmpty() && limitations.isNotEmpty()) {
            throw IOException("Could not enumerate resources for $description: ${limitations.joinToString("; ")}")
        }
        return resources.distinctBy { it.path to it.origin }.sortedBy { it.path }
    }

    private fun addRoot(roots: MutableMap<String, URL>, url: URL) {
        when (url.protocol) {
            "file" -> {
                val path = runCatching { Path.of(url.toURI()) }.getOrNull()
                if (path != null && Files.isRegularFile(path) && path.fileName.toString().endsWith(".jar", ignoreCase = true)) {
                    val jarRoot = runCatching { URI("jar:${url.toExternalForm()}!/").toURL() }.getOrNull()
                    if (jarRoot != null) roots.putIfAbsent(url.toExternalForm(), jarRoot)
                } else {
                    roots.putIfAbsent(url.toExternalForm(), url)
                }
            }
            "jar" -> {
                val connection = runCatching { url.openConnection() as? JarURLConnection }.getOrNull()
                if (connection != null) {
                    val jarRoot = runCatching { URI("jar:${connection.jarFileURL.toExternalForm()}!/").toURL() }.getOrNull()
                    if (jarRoot != null) roots.putIfAbsent(connection.jarFileURL.toExternalForm(), jarRoot)
                }
            }
        }
    }

    private fun sameJar(resource: URL, jarPath: Path): Boolean = runCatching {
        val connection = resource.openConnection() as JarURLConnection
        Path.of(connection.jarFileURL.toURI()).toAbsolutePath().normalize() == jarPath.toAbsolutePath().normalize()
    }.getOrDefault(false)
}