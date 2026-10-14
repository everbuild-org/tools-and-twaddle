package org.everbuild.twaddle.data_driven.resource

/** Discovers resources without interpreting their contents. */
interface DataSource {
    val description: String
    fun discover(): List<RawResource>
}

internal fun normalizeResourcePath(raw: String): String? {
    if (raw.isBlank() || raw.startsWith('/') || raw.startsWith('\\') || raw.contains('\\')) return null
    val segments = raw.split('/')
    if (segments.any { it.isEmpty() || it == "." || it == ".." }) return null
    return segments.joinToString("/")
}
