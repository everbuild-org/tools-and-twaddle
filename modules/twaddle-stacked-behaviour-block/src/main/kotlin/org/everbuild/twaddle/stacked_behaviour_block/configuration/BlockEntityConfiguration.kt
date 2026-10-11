package org.everbuild.twaddle.stacked_behaviour_block.configuration

import java.util.Collections
import net.minestom.server.tag.Tag
import org.everbuild.twaddle.stacked_behaviour.BehaviourEditor
import org.everbuild.twaddle.stacked_behaviour.dsl.StackedBehaviourDslMarker
import org.everbuild.twaddle.stacked_behaviour_block.ruleset.BlockRuleset

/** Shared handler settings. [tags] selects block entity data sent to clients; it does not initialize block data. */
class BlockEntityConfiguration @JvmOverloads constructor(
    val action: Byte = -1,
    tags: Collection<Tag<*>> = emptySet(),
) {
    val tags: Set<Tag<*>> = Collections.unmodifiableSet(LinkedHashSet(tags))
}

@StackedBehaviourDslMarker
class BlockEntityConfigurationEditor @JvmOverloads constructor(
    configuration: BlockEntityConfiguration = BlockEntityConfiguration(),
) {
    var action: Byte = configuration.action
    val tags: MutableSet<Tag<*>> = configuration.tags.toMutableSet()

    fun build(): BlockEntityConfiguration = BlockEntityConfiguration(action, tags)
}

/** Edits the existing settings and commits a snapshot only after [edit] succeeds. */
@JvmSynthetic
fun BehaviourEditor<BlockRuleset>.blockEntity(edit: BlockEntityConfigurationEditor.() -> Unit) {
    val existing = configuration(BlockEntityConfiguration::class.java) ?: BlockEntityConfiguration()
    val updated = BlockEntityConfigurationEditor(existing).apply(edit).build()
    configuration(BlockEntityConfiguration::class.java, updated)
}
