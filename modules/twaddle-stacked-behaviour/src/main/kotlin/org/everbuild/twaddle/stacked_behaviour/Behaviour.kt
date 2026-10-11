package org.everbuild.twaddle.stacked_behaviour

import java.util.Collections
import org.everbuild.twaddle.stacked_behaviour.engine.RuleChainApplicatorFactory

/** Immutable activity contributions for one subject in a ruleset. */
class Behaviour<R : StackedRuleset<R, *>> internal constructor(
    activities: Map<ActivityKey<R, *>, RuleChain<*>>,
) {
    internal val activities: Map<ActivityKey<R, *>, RuleChain<*>> =
        Collections.unmodifiableMap(LinkedHashMap(activities))

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

        /** Combines disjoint activities, including explicit empty contributions. */
        fun <R : StackedRuleset<R, *>> combine(vararg behaviours: Behaviour<R>): Behaviour<R> {
            val combined = linkedMapOf<ActivityKey<R, *>, RuleChain<*>>()
            for (behaviour in behaviours) {
                for ((key, chain) in behaviour.activities) {
                    require(key !in combined) { "Multiple contributions for activity $key" }
                    combined[key] = chain
                }
            }
            return Behaviour(combined)
        }
    }
}
