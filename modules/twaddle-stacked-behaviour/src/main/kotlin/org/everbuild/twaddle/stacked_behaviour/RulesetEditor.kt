package org.everbuild.twaddle.stacked_behaviour

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.stacked_behaviour.dsl.StackedBehaviourDslMarker

@StackedBehaviourDslMarker
open class RulesetEditor<R : StackedRuleset<R, *>>(
    definitions: Map<Key, Behaviour<R>> = emptyMap(),
) {
    private val definitions = LinkedHashMap(definitions)

    /** Creates missing subjects or edits existing ones independently, committing only if every edit succeeds. */
    fun behaviours(vararg keys: Key, edit: BehaviourEditor<R>.() -> Unit) {
        val selected = keys.distinct()
        val updates = selected.associateWith { key ->
            val editor = definitions[key]?.let { BehaviourEditor(it) } ?: BehaviourEditor<R>()
            editor.apply(edit).build()
        }
        definitions.putAll(updates)
    }

    fun remove(vararg keys: Key) {
        keys.forEach { key -> require(key in definitions) { "Behaviour $key is not present" } }
        keys.forEach(definitions::remove)
    }

    /** Keeps only selected subjects in their existing order. Missing keys are ignored; no keys clears the ruleset. */
    fun retainOnly(vararg keys: Key) {
        definitions.keys.retainAll(keys.toSet())
    }

    open fun build(): StackedRuleset<R, RulesetEditor<R>> = StackedRuleset.snapshot(definitions)
}
