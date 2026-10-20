package org.everbuild.twaddle.data_driven.loading

import org.everbuild.twaddle.data_driven.resource.RawResource

/** Converts one raw resource into a value. Implementations should include [RawResource.origin] in failures. */
fun interface DataLoader<T : Any> {
    fun load(resource: RawResource): T
}
