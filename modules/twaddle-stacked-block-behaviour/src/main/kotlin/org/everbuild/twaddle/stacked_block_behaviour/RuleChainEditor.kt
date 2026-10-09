package org.everbuild.twaddle.stacked_block_behaviour

import org.everbuild.twaddle.stacked_block_behaviour.dsl.RuleChainDsl
import org.everbuild.twaddle.stacked_block_behaviour.dsl.StackedBehaviourDslMarker

/**
 * Edits one activity contribution.
 *
 * Bare invocation opens the editor; additions and replacements use explicit verbs.
 */
@StackedBehaviourDslMarker
class RuleChainEditor<T : ActivityContext> internal constructor(
    chain: RuleChain<T>?,
) {
    private var contribution: List<Rule<T>>? = chain?.rules()?.toList()

    operator fun invoke(edit: RuleChainEditor<T>.() -> Unit) {
        edit(this)
    }

    /** Removes this activity's contribution entirely. */
    fun omit() {
        contribution = null
    }

    /** Provides an empty chain. */
    fun clear() {
        contribution = emptyList()
    }

    /** Replaces the entire chain, including an absent contribution. */
    fun replace(build: RuleChainDsl<T>.() -> Unit) {
        commit(newRules(build))
    }

    fun append(build: RuleChainDsl<T>.() -> Unit) {
        commit(contribution.orEmpty() + newRules(build))
    }

    fun prepend(build: RuleChainDsl<T>.() -> Unit) {
        commit(newRules(build) + contribution.orEmpty())
    }

    fun remove(rule: Rule<T>) = remove(rule.key)

    fun remove(key: Rule.Key<T>) {
        val (rules, index) = locate(key)
        val updated = rules.toMutableList()
        updated.removeAt(index)
        commit(updated)
    }

    fun before(
        anchor: Rule<T>,
        build: RuleChainDsl<T>.() -> Unit,
    ) = before(anchor.key, build)

    fun before(
        anchor: Rule.Key<T>,
        build: RuleChainDsl<T>.() -> Unit,
    ) {
        val (rules, index) = locate(anchor)
        insert(rules, index, newRules(build))
    }

    fun after(
        anchor: Rule<T>,
        build: RuleChainDsl<T>.() -> Unit,
    ) = after(anchor.key, build)

    fun after(
        anchor: Rule.Key<T>,
        build: RuleChainDsl<T>.() -> Unit,
    ) {
        val (rules, index) = locate(anchor)
        insert(rules, index + 1, newRules(build))
    }

    fun replace(
        target: Rule<T>,
        build: RuleChainDsl<T>.() -> Unit,
    ) = replace(target.key, build)

    fun replace(
        target: Rule.Key<T>,
        build: RuleChainDsl<T>.() -> Unit,
    ) {
        val (rules, index) = locate(target)
        val replacements = newRules(build)

        commit(
            rules.take(index) +
                    replacements +
                    rules.drop(index + 1)
        )
    }

    internal fun build(): RuleChain<T>? =
        contribution?.let { RuleChain(it.toList()) }

    private fun newRules(
        build: RuleChainDsl<T>.() -> Unit,
    ): List<Rule<T>> = RuleChain.build(build).rules()

    private fun locate(
        key: Rule.Key<T>,
    ): Pair<List<Rule<T>>, Int> {
        val rules = contribution.orEmpty()
        val index = rules.indexOfFirst { it.key == key }

        require(index >= 0) {
            "Rule $key is not present in this chain"
        }

        return rules to index
    }

    private fun insert(
        rules: List<Rule<T>>,
        index: Int,
        additions: List<Rule<T>>,
    ) {
        commit(rules.take(index) + additions + rules.drop(index))
    }

    private fun commit(rules: List<Rule<T>>) {
        val keys = hashSetOf<Rule.Key<T>>()

        rules.forEach { rule ->
            require(keys.add(rule.key)) {
                "Rule with key ${rule.key} already installed"
            }
        }

        contribution = rules.toList()
    }
}