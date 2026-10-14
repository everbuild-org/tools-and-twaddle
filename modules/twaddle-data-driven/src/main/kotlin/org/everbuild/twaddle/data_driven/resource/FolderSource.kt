package org.everbuild.twaddle.data_driven.resource

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

class FolderSource(private val root: Path) : DataSource {
    override val description: String = "folder:${root.toAbsolutePath().normalize()}"

    override fun discover(): List<RawResource> {
        val absoluteRoot = root.toAbsolutePath().normalize()
        if (!Files.exists(absoluteRoot)) return emptyList()
        require(Files.isDirectory(absoluteRoot)) { "Data folder is not a directory: $absoluteRoot" }
        val realRoot = try {
            absoluteRoot.toRealPath()
        } catch (failure: IOException) {
            throw IOException("Could not access data folder $absoluteRoot", failure)
        }
        return Files.walk(realRoot).use { paths ->
            paths.filter { Files.isRegularFile(it) }
                .sorted()
                .map { file ->
                    val normalized = try { file.toRealPath() } catch (failure: IOException) {
                        throw IOException("Could not resolve resource under $realRoot: $file", failure)
                    }
                    require(normalized.startsWith(realRoot)) { "Resource escaped configured folder: $file" }
                    val relative = realRoot.relativize(normalized).joinToString("/")
                    try {
                        RawResource(relative, "$description!/$relative", Files.readAllBytes(normalized))
                    } catch (failure: IOException) {
                        throw IOException("Could not read resource '$relative' from $description", failure)
                    }
                }.toList()
        }
    }
}