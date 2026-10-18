package org.everbuild.twaddle.datapack_primitives.slot_source.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSource
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceKeys
import org.everbuild.twaddle.datapack_primitives.slot_source.SlotSourceType

private data class ContentsSlotSource(val component: String, val slotSource: SlotSource) : SlotSource {
    override fun type() = SlotSourceKeys.CONTENTS

    override fun evaluate(context: DataContext): List<ItemStack> {
        val contents = when (component) {
            "minecraft:container" -> DataComponents.CONTAINER
            "minecraft:bundle_contents" -> DataComponents.BUNDLE_CONTENTS
            "minecraft:charged_projectiles" -> DataComponents.CHARGED_PROJECTILES
            else -> error("Unsupported item contents component: $component")
        }
        return slotSource.evaluate(context).flatMap { stack ->
            stack.get(contents).orEmpty()
        }
    }
}

internal data object ContentsSlotSourceType : SlotSourceType {
    override fun codec(slotSourceCodec: Codec<SlotSource>): StructCodec<out SlotSource> = StructCodec.struct(
        "component", Codec.STRING, { (it as ContentsSlotSource).component },
        "slot_source", slotSourceCodec, { (it as ContentsSlotSource).slotSource },
        ::ContentsSlotSource,
    )
}
