package org.everbuild.twaddle.datapack_primitives.slot_source

import net.kyori.adventure.key.Key
import net.minestom.server.registry.DynamicRegistry
import org.everbuild.twaddle.datapack_primitives.slot_source.impl.ContentsSlotSourceType
import org.everbuild.twaddle.datapack_primitives.slot_source.impl.EmptySlotSourceType
import org.everbuild.twaddle.datapack_primitives.slot_source.impl.FilteredSlotSourceType
import org.everbuild.twaddle.datapack_primitives.slot_source.impl.GroupSlotSourceType
import org.everbuild.twaddle.datapack_primitives.slot_source.impl.LimitSlotsSlotSourceType
import org.everbuild.twaddle.datapack_primitives.slot_source.impl.SlotRangeSlotSourceType

/** Registers all vanilla slot-source types. */
fun addAllVanillaSlotSourceTypes(registry: DynamicRegistry<SlotSourceType>) {
    registry.register(SlotSourceKeys.EMPTY, EmptySlotSourceType)
    registry.register(SlotSourceKeys.SLOT_RANGE, SlotRangeSlotSourceType)
    registry.register(SlotSourceKeys.CONTENTS, ContentsSlotSourceType)
    registry.register(SlotSourceKeys.FILTERED, FilteredSlotSourceType)
    registry.register(SlotSourceKeys.GROUP, GroupSlotSourceType)
    registry.register(SlotSourceKeys.LIMIT_SLOTS, LimitSlotsSlotSourceType)
}

@Suppress("UnstableApiUsage")
fun createVanillaSlotSourceTypeRegistry(): DynamicRegistry<SlotSourceType> =
    DynamicRegistry.create<SlotSourceType>(Key.key("twaddle:vanilla_slot_source_types")).also(
        ::addAllVanillaSlotSourceTypes,
    )
