package org.everbuild.twaddle.stacked_behaviour

import java.util.function.Function
import net.kyori.adventure.key.Key as AdventureKey
import net.kyori.adventure.key.KeyPattern

interface Rule<T : ActivityContext> {
    val key: Key<T>
    fun evaluate(context: T): RuleResult<T>

    @JvmRecord
    data class Key<T>(@KeyPattern.Namespace val namespace: String, @KeyPattern.Value val value: String) {
        constructor(key: AdventureKey) : this(key.namespace(), key.value())
        constructor(@KeyPattern value: String) : this(AdventureKey.key(value))
    }

    companion object {
        internal class RuleImpl<T : ActivityContext>(override val key: Rule.Key<T>, val block: (T) -> RuleResult<T>) :
            Rule<T> {
            override fun evaluate(context: T): RuleResult<T> = block(context)
        }

        operator fun <T : ActivityContext> invoke(key: Key<T>, block: (T) -> RuleResult<T>): Rule<T> =
            RuleImpl(key, block)

        operator fun <T : ActivityContext> invoke(@KeyPattern key: String, block: (T) -> RuleResult<T>): Rule<T> =
            RuleImpl(Key(key), block)

        @JvmSynthetic
        @JvmName("__kt_rule")
        fun <T : ActivityContext> rule(key: Key<T>, block: (T) -> RuleResult<T>): Rule<T> = invoke(key, block)

        @JvmSynthetic
        @JvmName("__kt_rule")
        fun <T : ActivityContext> rule(@KeyPattern key: String, block: (T) -> RuleResult<T>): Rule<T> = invoke(key, block)

        @JvmName("rule")
        @JvmStatic
        fun <T : ActivityContext> jvmRule(key: Key<T>, block: Function<T, RuleResult<T>>): Rule<T> =
            invoke(key) { block.apply(it) }

        @JvmName("rule")
        @JvmStatic
        fun <T : ActivityContext> jvmRule(@KeyPattern key: String, block: Function<T, RuleResult<T>>): Rule<T> =
            invoke(key) { block.apply(it) }
    }
}