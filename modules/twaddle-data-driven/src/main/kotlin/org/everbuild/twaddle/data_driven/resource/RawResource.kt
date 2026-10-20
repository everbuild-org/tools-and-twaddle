package org.everbuild.twaddle.data_driven.resource

/** A discovered resource with a normalized path, origin, and content available for this reload. */
data class RawResource(
    val path: String,
    val origin: String,
    val contents: ByteArray,
) {
    fun text(): String = contents.toString(Charsets.UTF_8)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RawResource

        if (path != other.path) return false
        if (origin != other.origin) return false
        if (!contents.contentEquals(other.contents)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = path.hashCode()
        result = 31 * result + origin.hashCode()
        result = 31 * result + contents.contentHashCode()
        return result
    }
}