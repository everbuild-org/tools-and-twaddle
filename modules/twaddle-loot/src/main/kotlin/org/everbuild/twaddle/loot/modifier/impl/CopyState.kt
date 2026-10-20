package org.everbuild.twaddle.loot.modifier.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class CopyStateLootModifier(val block: Key, val properties: List<String>) : LootModifier {
    override fun type() = LootModifierKeys.COPY_STATE
}

internal class CopyStateLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("block", Codec.KEY, { (it as CopyStateLootModifier).block }, "properties", Codec.STRING.list(), { (it as CopyStateLootModifier).properties }, ::CopyStateLootModifier)!!
}
