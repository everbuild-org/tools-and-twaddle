package org.everbuild.twaddle.loot.modifier.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class CopyComponentsLootModifier(val source: String, val include: List<Key>?, val exclude: List<Key>?) : LootModifier {
    override fun type() = LootModifierKeys.COPY_COMPONENTS
}

internal class CopyComponentsLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("source", Codec.STRING, { (it as CopyComponentsLootModifier).source }, "include", Codec.KEY.list().optional(), { (it as CopyComponentsLootModifier).include }, "exclude", Codec.KEY.list().optional(), { (it as CopyComponentsLootModifier).exclude }, ::CopyComponentsLootModifier)!!
}
