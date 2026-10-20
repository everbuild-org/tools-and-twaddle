package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class SetOminousBottleAmplifierLootModifier(val amplifier: NumberProviderDeclaration<Int>) : LootModifier {
    override fun type() = LootModifierKeys.SET_OMINOUS_BOTTLE_AMPLIFIER
}

internal class SetOminousBottleAmplifierLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("amplifier", intNumberProviderCodec, { (it as SetOminousBottleAmplifierLootModifier).amplifier }, ::SetOminousBottleAmplifierLootModifier)!!
}
