package org.everbuild.twaddle.data_driven.pattern

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.data_driven.resource.normalizeResourcePath
import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher

/**
 * A filesystem-style glob. The literal directory prefix before the first wildcard is removed
 * from matched paths, as is the literal suffix (commonly `.json`) after the last wildcard.
 * `*` matches within one path segment; `**` may match across segments.
 */
class GlobPattern(glob: String, private val namespace: String) : ResourcePattern {
    private val normalizedGlob: String
    private val matcher: PathMatcher
    private val idPrefix: String
    private val idSuffix: String

    init {
        require(glob.isNotBlank() && !glob.startsWith('/') && !glob.contains('\\')) { "Glob must be a normalized relative path" }
        normalizedGlob = glob.trimEnd('/')
        // Java's `**/` glob requires a directory segment; resource globs conventionally include
        // both the root-level and nested forms, so make that directory optional.
        val matcherGlob = normalizedGlob.replace("**/", "{,**/}")
        matcher = FileSystems.getDefault().getPathMatcher("glob:$matcherGlob")
        val firstWildcard = normalizedGlob.indexOfAny(charArrayOf('*', '?', '['))
        val prefixEnd = if (firstWildcard < 0) normalizedGlob.length else normalizedGlob.lastIndexOf('/', firstWildcard)
            .let { if (it < 0) 0 else it + 1 }

        idPrefix = normalizedGlob.substring(0, prefixEnd)
        val lastWildcard = normalizedGlob.lastIndexOfAny(charArrayOf('*', '?', '['))
        val suffixStart = lastWildcard + 1
        val suffix = if (firstWildcard < 0) "" else normalizedGlob.substring(suffixStart).takeWhile { it != '/' }
        val extensionStart = suffix.indexOf('.')
        idSuffix = if (extensionStart < 0) "" else suffix.substring(extensionStart)
        require(namespace.isNotBlank()) { "Namespace must not be blank" }
    }

    override fun key(path: String): Key? {
        val normalized = normalizeResourcePath(path) ?: return null
        if (!matcher.matches(Path.of(normalized))) return null
        if (!normalized.startsWith(idPrefix)) return null
        var resourcePath = normalized.removePrefix(idPrefix)
        if (idSuffix.isNotEmpty() && resourcePath.endsWith(idSuffix)) resourcePath = resourcePath.removeSuffix(idSuffix)
        return parseKeyOrNull("$namespace:$resourcePath")
    }
}