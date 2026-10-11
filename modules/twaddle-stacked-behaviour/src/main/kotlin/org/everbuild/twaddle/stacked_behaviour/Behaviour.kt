package org.everbuild.twaddle.stacked_behaviour

import java.util.Collections
import org.everbuild.twaddle.stacked_behaviour.engine.RuleChainApplicatorFactory

/** Immutable activity and configuration contributions for one subject in a ruleset. */
class Behaviour<R : StackedRuleset<R, *>> internal constructor(
    activities: Map<ActivityKey<R, *>, RuleChain<*>>,
    configurations: Map<Class<*>, Any> = emptyMap(),
) {
    internal val activities: Map<ActivityKey<R, *>, RuleChain<*>> =
        Collections.unmodifiableMap(LinkedHashMap(activities))

    internal val configurations: Map<Class<*>, Any> =
        Collections.unmodifiableMap(LinkedHashMap(configurations))

    /** Returns the configuration registered under this exact class, or null when absent. */
    fun <T : Any> configuration(type: Class<T>): T? = configurations[type]?.let(type::cast)

    /** Null means absent; an empty chain is an explicit contribution. */
    @Suppress("UNCHECKED_CAST")
    fun <C : ActivityContext> activity(key: ActivityKey<R, C>): RuleChain<C>? =
        activities[key] as RuleChain<C>?

    fun modify(edit: BehaviourEditor<R>.() -> Unit): Behaviour<R> =
        BehaviourEditor(this).apply(edit).build()

    fun <C : ActivityContext> createApplicator(
        key: ActivityKey<R, C>,
        factory: RuleChainApplicatorFactory,
    ): RuleChainApplicator<C> = activity(key)?.let { factory.create(it) } ?: RuleChainApplicator.empty()

    companion object {
        fun <R : StackedRuleset<R, *>> build(edit: BehaviourEditor<R>.() -> Unit): Behaviour<R> =
            BehaviourEditor<R>().apply(edit).build()

        /** Combines disjoint activities and configuration classes, including explicit empty chains. */
        fun <R : StackedRuleset<R, *>> combine(vararg behaviours: Behaviour<R>): Behaviour<R> {
            val combined = linkedMapOf<ActivityKey<R, *>, RuleChain<*>>()
            val configurations = linkedMapOf<Class<*>, Any>()
            for (behaviour in behaviours) {
                for ((key, chain) in behaviour.activities) {
                    require(key !in combined) { "Multiple contributions for activity $key" }
                    combined[key] = chain
                }
                for ((type, value) in behaviour.configurations) {
                    require(type !in configurations) { "Multiple contributions for configuration ${type.name}" }
                    configurations[type] = value
                }
            }
            return Behaviour(combined, configurations)
        }
    }
}
