package org.everbuild.twaddle.data_driven.loader

import com.google.gson.JsonParser
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import org.everbuild.twaddle.data_driven.loading.DataLoader
import org.everbuild.twaddle.data_driven.resource.RawResource

/** Decodes a JSON resource through Minestom's codec and JSON transcoder. */
class CodecLoader<T : Any>(private val codec: Codec<T>) : DataLoader<T> {
    override fun load(resource: RawResource): T {
        val json = try {
            JsonParser.parseString(resource.text())
        } catch (failure: RuntimeException) {
            throw IllegalArgumentException("Invalid JSON in ${resource.origin}: ${failure.message}", failure)
        }
        return when (val decoded = codec.decode(Transcoder.JSON, json)) {
            is Result.Ok -> decoded.value
            is Result.Error -> throw IllegalArgumentException("Could not decode ${resource.origin}: ${decoded.message}")
        }
    }
}
