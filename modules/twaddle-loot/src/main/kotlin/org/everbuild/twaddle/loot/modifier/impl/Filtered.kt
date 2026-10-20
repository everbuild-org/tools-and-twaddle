package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class FilteredLootModifier(val itemFilter: Codec.RawValue, val onFail: LootModifier) : LootModifier {
    override fun type() = LootModifierKeys.FILTERED
}

internal class FilteredLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("item_filter", Codec.RAW_VALUE, { (it as FilteredLootModifier).itemFilter }, "on_fail", modifierCodec, { (it as FilteredLootModifier).onFail }, ::FilteredLootModifier)!!
}
