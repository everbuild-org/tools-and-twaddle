package org.everbuild.twaddle.datapack_primitives.number_provider.context

import net.kyori.adventure.key.Key

/**
 * Typed token binding a value T to a [NumberProviderContext]
 *
 * Declare each key once and reuse it. Keys use identity equality: the textual [id]
 * is a diagnostic label, so differently typed tokens cannot alias through an ID.
 */
class NumberProviderContextKey<T>(val id: Key) {
    constructor(id: String) : this(Key.key(id))
    override fun toString(): String = id.asString()
}