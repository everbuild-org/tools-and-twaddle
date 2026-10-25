package org.everbuild.twaddle.loot.modifier.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.component.ItemBlockState
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootBlockStateContext
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import java.util.Random

data class CopyStateLootModifier(val block: Key, val properties: List<String>) : LootModifier {
    override fun type() = LootModifierKeys.COPY_STATE
}

internal class CopyStateLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("block", Codec.KEY, { (it as CopyStateLootModifier).block }, "properties", Codec.STRING.list(), { (it as CopyStateLootModifier).properties }, ::CopyStateLootModifier)!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val copyState = modifier as CopyStateLootModifier
        val sourceBlock = context[LootBlockStateContext].blockState() ?: return item
        if (sourceBlock.key() != copyState.block) return item

        val selectedProperties = copyState.properties.toSet()
        val copiedProperties = sourceBlock.properties().filterKeys(selectedProperties::contains)
        if (copiedProperties.isEmpty()) return item

        val itemState = item.get(DataComponents.BLOCK_STATE) ?: ItemBlockState.EMPTY
        val updatedState = copiedProperties.entries.fold(itemState) { state, (key, value) -> state.with(key, value) }
        return item.with(DataComponents.BLOCK_STATE, updatedState)
    }
}
