package org.everbuild.twaddle.stacked_behaviour

import java.util.Collections
import net.kyori.adventure.key.Key

/** Immutable behaviours indexed by subject identifiers, with activity domain [R] and editor type [E]. */
open class StackedRuleset<R : StackedRuleset<R, *>, out E : RulesetEditor<R>> protected constructor(
    definitions: Map<Key, Behaviour<R>>,
    private val editorFactory: (Map<Key, Behaviour<R>>) -> E,
) {
    val behaviours: Map<Key, Behaviour<R>> =
        Collections.unmodifiableMap(LinkedHashMap(definitions))

    operator fun get(key: Key): Behaviour<R>? = behaviours[key]

    @JvmSynthetic
    open fun modify(edit: E.() -> Unit): StackedRuleset<R, RulesetEditor<R>> =
        editorFactory(behaviours).apply(edit).build()

    companion object {
        internal fun <R : StackedRuleset<R, *>> snapshot(definitions: Map<Key, Behaviour<R>>): StackedRuleset<R, RulesetEditor<R>> =
            StackedRuleset(definitions, ::RulesetEditor)

        fun <R : StackedRuleset<R, *>> build(edit: RulesetEditor<R>.() -> Unit): StackedRuleset<R, RulesetEditor<R>> =
            RulesetEditor<R>().apply(edit).build()

        fun <R : StackedRuleset<R, *>> combine(vararg rulesets: StackedRuleset<R, *>): StackedRuleset<R, RulesetEditor<R>> {
            val combined = linkedMapOf<Key, Behaviour<R>>()
            for (ruleset in rulesets) {
                for ((key, incoming) in ruleset.behaviours) {
                    val existing = combined[key]
                    combined[key] = if (existing == null) incoming else Behaviour.combine(existing, incoming)
                }
            }
            return StackedRuleset(combined, ::RulesetEditor)
        }
    }
}
