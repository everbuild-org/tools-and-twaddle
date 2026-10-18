package org.everbuild.twaddle.datapack_primitives.context

import net.minestom.server.codec.Codec
import net.minestom.server.item.ItemStack

/** Reads one stack per selected slot. Return [ItemStack.AIR] for empty slots to preserve slot positions. */
fun interface SlotSourceInventoryAccess {
    fun getItems(entitySource: String, slotPatterns: List<String>): List<ItemStack>
}

/** Matches a raw item predicate against an item stack. */
fun interface ItemPredicateMatcher {
    fun matches(item: ItemStack, predicate: Codec.RawValue): Boolean
}

val SlotSourceInventoryContext = DataContextKey<SlotSourceInventoryAccess>("twaddle:slot_source_inventory_context")
val ItemPredicateMatcherContext = DataContextKey<ItemPredicateMatcher>("twaddle:item_predicate_matcher_context")

@JvmSynthetic
fun DataContext.withSlotSourceInventoryAccess(access: SlotSourceInventoryAccess) =
    withBehaviour(SlotSourceInventoryContext, access)

@JvmSynthetic
fun DataContext.withItemPredicateMatcher(matcher: ItemPredicateMatcher) =
    withBehaviour(ItemPredicateMatcherContext, matcher)
