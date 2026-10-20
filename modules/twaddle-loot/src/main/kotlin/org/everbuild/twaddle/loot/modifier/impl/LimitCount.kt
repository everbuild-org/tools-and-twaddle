package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class LimitCountLootModifier(val limit: Codec.RawValue) : LootModifier {
    override fun type() = LootModifierKeys.LIMIT_COUNT
}

internal class LimitCountLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("limit", Codec.RAW_VALUE, { (it as LimitCountLootModifier).limit }, ::LimitCountLootModifier)!!
}
