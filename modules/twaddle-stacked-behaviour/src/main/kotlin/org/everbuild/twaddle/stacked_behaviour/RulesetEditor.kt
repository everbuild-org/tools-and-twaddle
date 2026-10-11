package org.everbuild.twaddle.stacked_behaviour

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.stacked_behaviour.dsl.StackedBehaviourDslMarker

@StackedBehaviourDslMarker
open class RulesetEditor<R : StackedRuleset<R, *>>(
    definitions: Map<Key, Behaviour<R>> = emptyMap(),
) {
    private val definitions = LinkedHashMap(definitions)

    /** Adds a new subject. Existing subjects must be edited explicitly. */
    fun add(key: Key, edit: BehaviourEditor<R>.() -> Unit) {
        require(key !in definitions) { "Behaviour $key already exists" }
        definitions[key] = Behaviour.build(edit)
    }

    /** Edits each selected subject independently and commits only if every edit succeeds. */
    fun behaviours(vararg keys: Key, edit: BehaviourEditor<R>.() -> Unit) {
        val selected = keys.distinct()
        selected.forEach { key -> require(key in definitions) { "Behaviour $key is not present" } }
        val updates = selected.associateWith { key ->
            BehaviourEditor(definitions.getValue(key)).apply(edit).build()
        }
        definitions.putAll(updates)
    }

    fun remove(vararg keys: Key) {
        keys.forEach { key -> require(key in definitions) { "Behaviour $key is not present" } }
        keys.forEach(definitions::remove)
    }

    open fun build(): StackedRuleset<R, RulesetEditor<R>> = StackedRuleset.snapshot(definitions)
}
