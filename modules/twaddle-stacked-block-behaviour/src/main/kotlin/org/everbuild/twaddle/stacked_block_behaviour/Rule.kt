package org.everbuild.twaddle.stacked_block_behaviour

import net.kyori.adventure.key.KeyPattern
import net.kyori.adventure.key.Key as AdventureKey

interface Rule<T : ActivityContext> {
    val key: Key<T>
    fun evaluate(context: T): RuleResult<T>

    @JvmRecord
    data class Key<T>(@KeyPattern.Namespace val namespace: String, @KeyPattern.Value val value: String) {
        constructor(key: AdventureKey) : this(key.namespace(), key.value())
        constructor(@KeyPattern value: String) : this(AdventureKey.key(value))
    }
}