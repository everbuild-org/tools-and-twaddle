package org.everbuild.twaddle.loot.modifier.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
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
            StructCodec.struct("type", Codec.KEY, StewEffect::type, "duration", intNumberProviderCodec, StewEffect::duration, ::StewEffect).list(),
            { (it as SetStewEffectLootModifier).effects },
            ::SetStewEffectLootModifier,
        )!!
}
