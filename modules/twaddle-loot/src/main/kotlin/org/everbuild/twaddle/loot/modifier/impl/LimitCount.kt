package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.item.ItemStack
import net.minestom.server.utils.Either
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

sealed interface ItemCountLimit {
    data class Exact(val count: Int) : ItemCountLimit

    data class Range(
        val min: NumberProviderDeclaration<Int>? = null,
        val max: NumberProviderDeclaration<Int>? = null,
    ) : ItemCountLimit {
        init {
            require(min != null || max != null) { "At least one of min or max must be specified" }
        }
    }
}

data class LimitCountLootModifier(val limit: ItemCountLimit) : LootModifier {
    override fun type() = LootModifierKeys.LIMIT_COUNT
}

internal class LimitCountLootModifierType : LootModifierType {
    private fun rangeCodec(intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>) = StructCodec.struct(
        "min", intNumberProviderCodec.optional(), { it.min },
        "max", intNumberProviderCodec.optional(), { it.max },
        { min, max -> ItemCountLimit.Range(min, max) },
    )

    override fun codec(
        modifierCodec: Codec<LootModifier>,
        predicateCodec: Codec<ContextPredicate>,
        intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ) = StructCodec.struct(
        "limit",
        Codec.Either(Codec.INT, rangeCodec(intNumberProviderCodec)).transform(
            { either -> either.unify<ItemCountLimit>({ ItemCountLimit.Exact(it) }, { it }) },
            { limit ->
                when (limit) {
                    is ItemCountLimit.Exact -> Either.left<Int, ItemCountLimit.Range>(limit.count)
                    is ItemCountLimit.Range -> Either.right<Int, ItemCountLimit.Range>(limit)
                }
            },
        ),
        { (it as LimitCountLootModifier).limit },
        ::LimitCountLootModifier,
    )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val limit = (modifier as LimitCountLootModifier).limit
        val min: Int?
        val max: Int?
        when (limit) {
            is ItemCountLimit.Exact -> {
                min = limit.count
                max = limit.count
            }
            is ItemCountLimit.Range -> {
                min = limit.min?.get(context, random)
                max = limit.max?.get(context, random)
            }
        }
        require(min == null || max == null || min <= max) { "limit_count minimum cannot exceed maximum" }
        val count = item.amount().let { amount ->
            max?.let { amount.coerceAtMost(it) } ?: amount
        }.let { amount ->
            min?.let { amount.coerceAtLeast(it) } ?: amount
        }
        return if (count <= 0) ItemStack.AIR else item.withAmount(count)
    }
}
