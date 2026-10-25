package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.SuspiciousStewEffects
import net.minestom.server.potion.PotionEffect
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class StewEffect(val type: Key, val duration: NumberProviderDeclaration<Int>)

data class SetStewEffectLootModifier(val effects: List<StewEffect>) : LootModifier {
    override fun type() = LootModifierKeys.SET_STEW_EFFECT
}

internal class SetStewEffectLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct(
            "effects",
            StructCodec.struct("type", Codec.KEY, StewEffect::type, "duration", intNumberProviderCodec, StewEffect::duration, ::StewEffect)
                .list()
                .optional(emptyList()),
            { (it as SetStewEffectLootModifier).effects },
            ::SetStewEffectLootModifier,
        )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as SetStewEffectLootModifier
        if (item.material() != Material.SUSPICIOUS_STEW || declaration.effects.isEmpty()) return item

        val selected = declaration.effects[random.nextInt(declaration.effects.size)]
        val effect = PotionEffect.fromKey(selected.type)
            ?: error("No potion effect registered for ${selected.type}")
        val providerDuration = selected.duration.get(context, random)
        val durationTicks = if (effect.instantaneous()) providerDuration else providerDuration * 20
        val stewEffects = item.get(DataComponents.SUSPICIOUS_STEW_EFFECTS) ?: SuspiciousStewEffects.EMPTY
        return item.with(
            DataComponents.SUSPICIOUS_STEW_EFFECTS,
            stewEffects.with(SuspiciousStewEffects.Effect(effect, durationTicks)),
        )
    }
}
