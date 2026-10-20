package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.EnchantmentLevelContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class TableBonusPredicate(val enchantment: Key, val chances: List<Float>) : ContextPredicate {
    override fun type() = ContextPredicateKeys.TABLE_BONUS

    override fun test(context: DataContext, random: Random): Boolean {
        if (chances.isEmpty()) return false
        val level = context[EnchantmentLevelContext]?.enchantments()?.entries
            ?.firstOrNull { it.key.key() == enchantment }?.value ?: 0
        val chance = chances[level.coerceAtMost(chances.lastIndex)]
        return random.nextFloat() < chance
    }
}

internal class TableBonusPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "enchantment", Codec.KEY, TableBonusPredicate::enchantment,
        "chances", Codec.FLOAT.list(), TableBonusPredicate::chances,
        ::TableBonusPredicate,
    ).asContextPredicateCodec()
}
