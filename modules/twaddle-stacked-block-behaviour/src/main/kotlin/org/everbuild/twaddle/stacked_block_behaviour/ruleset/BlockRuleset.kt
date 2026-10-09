package org.everbuild.twaddle.stacked_block_behaviour.ruleset

import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_block_behaviour.dsl.StackedBehaviourDslMarker
import java.util.*
import kotlin.collections.iterator

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

    companion object : BlockRulesetApplicator() {
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
