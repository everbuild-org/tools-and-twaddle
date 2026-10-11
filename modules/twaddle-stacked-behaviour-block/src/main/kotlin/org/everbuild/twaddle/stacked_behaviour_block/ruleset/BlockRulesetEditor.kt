package org.everbuild.twaddle.stacked_behaviour_block.ruleset

import java.util.function.Consumer
import net.kyori.adventure.key.Key
import net.minestom.server.instance.block.Block
import org.everbuild.twaddle.stacked_behaviour.Behaviour
import org.everbuild.twaddle.stacked_behaviour.BehaviourEditor
import org.everbuild.twaddle.stacked_behaviour.RulesetEditor

class BlockRulesetEditor @JvmOverloads constructor(
    definitions: Map<Key, Behaviour<BlockRuleset>> = emptyMap(),
) : RulesetEditor<BlockRuleset>(definitions) {
    @JvmSynthetic
    fun addBlock(key: Key, edit: BehaviourEditor<BlockRuleset>.() -> Unit) = add(key, edit)

    @JvmSynthetic
    fun addBlock(block: Block, edit: BehaviourEditor<BlockRuleset>.() -> Unit) = add(block.key(), edit)

    fun addBlock(key: Key, edit: Consumer<BehaviourEditor<BlockRuleset>>) = addBlock(key) { edit.accept(this) }

    fun addBlock(block: Block, edit: Consumer<BehaviourEditor<BlockRuleset>>) = addBlock(block) { edit.accept(this) }

    @JvmSynthetic
    fun blocks(vararg keys: Key, edit: BehaviourEditor<BlockRuleset>.() -> Unit) = behaviours(*keys, edit = edit)

    @JvmSynthetic
    fun blocks(vararg blocks: Block, edit: BehaviourEditor<BlockRuleset>.() -> Unit) =
        behaviours(*blocks.map { it.key() }.toTypedArray(), edit = edit)

    // Put the callback first so Java callers can use a real varargs selection.
    fun blocks(edit: Consumer<BehaviourEditor<BlockRuleset>>, vararg keys: Key) =
        blocks(*keys, edit = { edit.accept(this) })

    fun blocks(edit: Consumer<BehaviourEditor<BlockRuleset>>, vararg blocks: Block) =
        blocks(*blocks, edit = { edit.accept(this) })

    fun removeBlocks(vararg keys: Key) = remove(*keys)

    fun removeBlocks(vararg blocks: Block) = remove(*blocks.map { it.key() }.toTypedArray())

    override fun build(): BlockRuleset = BlockRuleset(super.build().behaviours)
}
