package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.ItemPredicateMatcherContext
import org.everbuild.twaddle.loot.modifier.LootModifierRegistryContext
import org.everbuild.twaddle.loot.modifier.applyLootModifier
import net.minestom.server.item.ItemStack
import java.util.Random

data class FilteredLootModifier(
    val itemFilter: Codec.RawValue,
    val onPass: List<LootModifier> = emptyList(),
    val onFail: List<LootModifier> = emptyList(),
) : LootModifier {
    constructor(itemFilter: Codec.RawValue, onFail: LootModifier) : this(itemFilter, emptyList(), listOf(onFail))

    override fun type() = LootModifierKeys.FILTERED
}

internal class FilteredLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct(
            "item_filter", Codec.RAW_VALUE, { (it as FilteredLootModifier).itemFilter },
            "on_pass", modifierCodec.listOrSingle().optional(emptyList()), { (it as FilteredLootModifier).onPass },
            "on_fail", modifierCodec.listOrSingle().optional(emptyList()), { (it as FilteredLootModifier).onFail },
            { itemFilter, onPass, onFail -> FilteredLootModifier(itemFilter, onPass ?: emptyList(), onFail ?: emptyList()) },
        )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack? {
        val filtered = modifier as FilteredLootModifier
        val nestedModifiers = if (context[ItemPredicateMatcherContext].matches(item, filtered.itemFilter)) {
            filtered.onPass
        } else {
            filtered.onFail
        }

        val registry = context[LootModifierRegistryContext]
        var result: ItemStack? = item
        for (nestedModifier in nestedModifiers) {
            result = applyLootModifier(nestedModifier, result ?: return null, registry, context, random)
                ?: return null
        }
        return result
    }
}
