package org.everbuild.twaddle.data_driven.resource

import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipFile

class ZipSource(private val archive: Path) : DataSource {
    override val description: String = "zip:${archive.toAbsolutePath().normalize()}"

    override fun discover(): List<RawResource> {
        if (!Files.exists(archive)) return emptyList()
        try {
            ZipFile(archive.toFile()).use { zip ->
                return zip.entries().asSequence()
                    .filterNot { it.isDirectory }
                    .map { entry ->
                        val path = normalizeResourcePath(entry.name)
                            ?: throw IOException("Invalid resource path '${entry.name}' in $description")
                        val content = zip.getInputStream(entry).use { it.readBytes() }
                        RawResource(path, "$description!/$path", content)
                    }.toList()
            }
        } catch (failure: IOException) {
            throw IOException("Could not read data archive $description", failure)
        }
    }
}