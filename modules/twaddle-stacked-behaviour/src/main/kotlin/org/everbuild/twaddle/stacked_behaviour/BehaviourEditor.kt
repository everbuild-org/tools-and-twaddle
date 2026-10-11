package org.everbuild.twaddle.stacked_behaviour

import org.everbuild.twaddle.stacked_behaviour.dsl.StackedBehaviourDslMarker

@StackedBehaviourDslMarker
class BehaviourEditor<R : StackedRuleset<R, *>>(
    behaviour: Behaviour<R> = Behaviour(emptyMap()),
) {
    private val activities = behaviour.activities.mapValues { (_, chain) -> RuleChainEditor(chain) }.toMutableMap()
    private val configurations = behaviour.configurations.toMutableMap()

    fun <T : Any> configuration(type: Class<T>): T? = configurations[type]?.let(type::cast)

    /** Sets an immutable configuration value under the supplied class, replacing any existing value. */
    fun <T : Any> configuration(type: Class<T>, value: T) {
        configurations[type] = type.cast(value)
    }

    /** Removes this exact class's contribution. Removing an absent configuration has no effect. */
    fun removeConfiguration(type: Class<*>) {
        configurations.remove(type)
    }

    // ActivityKey is an identity token, and only this typed method inserts new editors.
    @Suppress("UNCHECKED_CAST")
    fun <C : ActivityContext> activity(key: ActivityKey<R, C>): RuleChainEditor<C> =
        activities.getOrPut(key) { RuleChainEditor<C>(null) } as RuleChainEditor<C>

    fun build(): Behaviour<R> = Behaviour(
        activities.mapNotNull { (key, editor) -> editor.build()?.let { key to it } }.toMap(),
        configurations,
    )
}
