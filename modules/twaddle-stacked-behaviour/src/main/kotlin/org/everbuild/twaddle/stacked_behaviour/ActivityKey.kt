package org.everbuild.twaddle.stacked_behaviour

import net.kyori.adventure.key.Key

/**
 * Typed token binding an activity to its ruleset domain and context.
 *
 * Declare each key once and reuse it. Keys use identity equality: the textual [id]
 * is a diagnostic label, so differently typed tokens cannot alias through an ID.
 */
class ActivityKey<R : StackedRuleset<R, *>, C : ActivityContext>(val id: Key) {
    constructor(id: String) : this(Key.key(id))

    override fun toString(): String = id.asString()
}
