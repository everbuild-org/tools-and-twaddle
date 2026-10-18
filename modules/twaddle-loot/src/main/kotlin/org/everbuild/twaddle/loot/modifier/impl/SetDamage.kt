package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import org.everbuild.twaddle.datapack_primitives.context.DataContext

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import kotlin.math.floor

data class SetDamageLootModifier(val damage: NumberProviderDeclaration<Float>, val add: Boolean = false) : LootModifier {
    override fun type() = LootModifierKeys.SET_DAMAGE
}

internal class SetDamageLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct(
            "damage", floatNumberProviderCodec, { (it as SetDamageLootModifier).damage },
            "add", Codec.BOOLEAN.optional(), { (it as SetDamageLootModifier).add.takeIf { add -> add } },
            { damage, add -> SetDamageLootModifier(damage, add ?: false) },
        )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val maxDamage = item.get(DataComponents.MAX_DAMAGE) ?: return item
        if (maxDamage <= 0) return item
        val declaration = modifier as SetDamageLootModifier
        val base = if (declaration.add) 1f - (item.get(DataComponents.DAMAGE) ?: 0).toFloat() / maxDamage else 0f
        val damageRatio = 1f - (declaration.damage.get(context, random) + base).coerceIn(0f, 1f)
        return item.with(DataComponents.DAMAGE, floor(damageRatio * maxDamage).toInt())
    }
}
