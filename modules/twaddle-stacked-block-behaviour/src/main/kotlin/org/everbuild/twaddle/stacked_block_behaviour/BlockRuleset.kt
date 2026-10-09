package org.everbuild.twaddle.stacked_block_behaviour

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_block_behaviour.dsl.RuleChainDsl
import org.everbuild.twaddle.stacked_block_behaviour.dsl.StackedBehaviourDslMarker
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext
import java.util.*

/**
 * Behavior contributions for one block type.
 *
 * A null chain means no contribution for that activity.
 * An empty chain is an explicit contribution that falls through.
 */
class BlockBehaviour(
    val interact: RuleChain<InteractionActivityContext>? = null,
)

/**
 * Immutable mapping from block identifiers to behavior definitions.
 */
class BlockRuleset private constructor(
    definitions: Map<Key, BlockBehaviour>,
) {
    val blocks: Map<Key, BlockBehaviour> =
        Collections.unmodifiableMap(LinkedHashMap(definitions))

    operator fun get(key: Key): BlockBehaviour? = blocks[key]

    fun modify(edit: BlockRulesetEditor.() -> Unit): BlockRuleset =
        BlockRulesetEditor(blocks).apply(edit).build()

    companion object {
        fun build(edit: BlockRulesetEditor.() -> Unit): BlockRuleset =
            BlockRulesetEditor(emptyMap()).apply(edit).build()

        /**
         * Combines disjoint contributions.
         *
         * Multiple rulesets may contribute to the same block, but not to
         * the same activity on that block.
         */
        fun combine(vararg rulesets: BlockRuleset): BlockRuleset {
            val combined = linkedMapOf<Key, BlockBehaviour>()

            for (ruleset in rulesets) {
                for ((key, incoming) in ruleset.blocks) {
                    val existing = combined[key]

                    require(
                        existing?.interact == null || incoming.interact == null
                    ) {
                        "Multiple interaction contributions for $key"
                    }

                    combined[key] = BlockBehaviour(
                        interact = incoming.interact ?: existing?.interact,
                    )
                }
            }

            return BlockRuleset(combined)
        }
    }

    @StackedBehaviourDslMarker
    class BlockRulesetEditor internal constructor(
        definitions: Map<Key, BlockBehaviour>,
    ) {
        private val definitions = LinkedHashMap(definitions)

        /** Adds a new block entry. Existing entries must be edited explicitly. */
        fun addBlock(
            key: Key,
            edit: BlockBehaviourEditor.() -> Unit,
        ) {
            require(key !in definitions) {
                "Block $key already exists"
            }

            definitions[key] = BlockBehaviourEditor(BlockBehaviour())
                .apply(edit)
                .build()
        }

        fun addBlock(
            block: Block,
            edit: BlockBehaviourEditor.() -> Unit,
        ) = addBlock(block.key(), edit)

        /** Edits existing entries independently for each selected block. */
        fun blocks(
            vararg keys: Key,
            edit: BlockBehaviourEditor.() -> Unit,
        ) {
            val selected = keys.distinct()

            selected.forEach { key ->
                require(key in definitions) {
                    "Block $key is not present"
                }
            }

            // Commit only after every selected edit succeeds.
            val updates = selected.associateWith { key ->
                BlockBehaviourEditor(definitions.getValue(key))
                    .apply(edit)
                    .build()
            }

            definitions.putAll(updates)
        }

        fun blocks(
            vararg blocks: Block,
            edit: BlockBehaviourEditor.() -> Unit,
        ) = blocks(
            *blocks.map { it.key() }.toTypedArray(),
            edit = edit,
        )

        fun removeBlocks(vararg keys: Key) {
            keys.forEach { key ->
                require(key in definitions) {
                    "Block $key is not present"
                }
            }

            keys.forEach(definitions::remove)
        }

        fun removeBlocks(vararg blocks: Block) =
            removeBlocks(*blocks.map { it.key() }.toTypedArray())

        internal fun build(): BlockRuleset = BlockRuleset(definitions)
    }
}

@StackedBehaviourDslMarker
class BlockBehaviourEditor internal constructor(
    behaviour: BlockBehaviour,
) {
    val interact = RuleChainEditor(behaviour.interact)

    internal fun build(): BlockBehaviour =
        BlockBehaviour(interact = interact.build())
}

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