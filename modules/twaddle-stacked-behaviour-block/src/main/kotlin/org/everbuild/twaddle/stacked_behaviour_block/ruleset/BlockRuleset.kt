package org.everbuild.twaddle.stacked_behaviour_block.ruleset

import java.util.function.Consumer
import net.kyori.adventure.key.Key
import org.everbuild.twaddle.stacked_behaviour.Behaviour
import org.everbuild.twaddle.stacked_behaviour.StackedRuleset

/** A stacked ruleset whose subjects are block identifiers. */
class BlockRuleset internal constructor(
    definitions: Map<Key, Behaviour<BlockRuleset>>,
) : StackedRuleset<BlockRuleset, BlockRulesetEditor>(definitions, ::BlockRulesetEditor) {
    val blocks: Map<Key, Behaviour<BlockRuleset>> get() = behaviours

    @JvmSynthetic
    override fun modify(edit: BlockRulesetEditor.() -> Unit): BlockRuleset =
        BlockRulesetEditor(behaviours).apply(edit).build()

    fun modify(edit: Consumer<BlockRulesetEditor>): BlockRuleset = modify { edit.accept(this) }

    companion object : BlockRulesetApplicator() {
        @JvmSynthetic
        fun build(edit: BlockRulesetEditor.() -> Unit): BlockRuleset =
            BlockRulesetEditor().apply(edit).build()

        @JvmStatic
        fun build(edit: Consumer<BlockRulesetEditor>): BlockRuleset = build { edit.accept(this) }

        @JvmStatic
        fun combine(vararg rulesets: BlockRuleset): BlockRuleset =
            BlockRuleset(StackedRuleset.combine(*rulesets).behaviours)
    }
}
