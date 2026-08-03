package org.everbuild.twaddle.core.minestom

import net.minestom.server.event.Event
import net.minestom.server.event.EventNode

inline fun <reified T : Event, V : EventNode<in T>> V.listen(noinline callback: (T) -> Unit): V {
    this.addListener(T::class.java, callback)
    return this
}
