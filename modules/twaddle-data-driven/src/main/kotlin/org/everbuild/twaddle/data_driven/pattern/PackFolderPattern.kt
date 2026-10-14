package org.everbuild.twaddle.data_driven.pattern

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.data_driven.resource.normalizeResourcePath

/** Maps `<prefix>/<namespace>/<category>/<resource-path><extension>` to a namespaced key. */
class PackFolderPattern(prefix: String = "data") {
    private val normalizedPrefix = prefix.trim('/').also {
        require(it.isNotBlank() && !it.contains('\\') && it.split('/').none { part -> part == "." || part == ".." || part.isEmpty() }) {
            "Prefix must be a normalized relative path"
        }
    }

    fun resource(category: String, extension: String): ResourcePattern {
        require(category.isNotBlank() && !category.startsWith('/') && !category.contains('\\') && category.split('/').none { it == "." || it == ".." || it.isEmpty() }) {
            "Category must be a normalized relative path"
        }
        require(extension.isNotBlank() && !extension.contains('/') && !extension.contains('\\')) { "Extension must not contain path separators" }
        val normalizedExtension = if (extension.startsWith('.')) extension else ".$extension"
        val requiredPrefix = "$normalizedPrefix/"
        return object : ResourcePattern {
            override fun key(path: String): Key? {
                val normalized = normalizeResourcePath(path) ?: return null
                if (!normalized.startsWith(requiredPrefix) || !normalized.endsWith(normalizedExtension)) return null
                val remainder = normalized.removePrefix(requiredPrefix).removeSuffix(normalizedExtension)
                val segments = remainder.split('/')
                if (segments.size < 3 || segments[1] != category) return null
                return parseKeyOrNull("${segments[0]}:${segments.drop(2).joinToString("/" )}")
            }
        }
    }
}