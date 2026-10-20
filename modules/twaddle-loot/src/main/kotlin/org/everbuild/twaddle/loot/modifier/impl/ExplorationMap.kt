package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class ExplorationMapLootModifier(val decoration: String, val destination: String) : LootModifier {
    override fun type() = LootModifierKeys.EXPLORATION_MAP
}

internal class ExplorationMapLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("decoration", Codec.STRING, { (it as ExplorationMapLootModifier).decoration }, "destination", Codec.STRING, { (it as ExplorationMapLootModifier).destination }, ::ExplorationMapLootModifier)!!
}
