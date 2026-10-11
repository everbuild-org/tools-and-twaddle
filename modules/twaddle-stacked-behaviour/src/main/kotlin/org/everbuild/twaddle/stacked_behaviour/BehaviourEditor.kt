package org.everbuild.twaddle.stacked_behaviour

import org.everbuild.twaddle.stacked_behaviour.dsl.StackedBehaviourDslMarker

@StackedBehaviourDslMarker
class BehaviourEditor<R : StackedRuleset<R, *>>(
    behaviour: Behaviour<R> = Behaviour(emptyMap()),
) {
    private val activities = behaviour.activities.mapValues { (_, chain) -> RuleChainEditor(chain) }.toMutableMap()

    // ActivityKey is an identity token, and only this typed method inserts new editors.
    @Suppress("UNCHECKED_CAST")
    fun <C : ActivityContext> activity(key: ActivityKey<R, C>): RuleChainEditor<C> =
        activities.getOrPut(key) { RuleChainEditor<C>(null) } as RuleChainEditor<C>

    fun build(): Behaviour<R> = Behaviour(
        activities.mapNotNull { (key, editor) -> editor.build()?.let { key to it } }.toMap()
    )
}
