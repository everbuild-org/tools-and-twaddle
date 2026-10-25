package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.kyori.adventure.text.Component
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

data class SetNameLootModifier(val name: Component? = null, val target: String? = null) : LootModifier {
    override fun type() = LootModifierKeys.SET_NAME
}

internal class SetNameLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("name", Codec.COMPONENT.optional(), { (it as SetNameLootModifier).name }, "target", Codec.STRING.optional(), { (it as SetNameLootModifier).target }, ::SetNameLootModifier)!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as SetNameLootModifier
        val name = declaration.name ?: return item
        return when (declaration.target ?: "custom_name") {
            "custom_name" -> item.with(DataComponents.CUSTOM_NAME, name)
            "item_name" -> item.with(DataComponents.ITEM_NAME, name)
            else -> error("Unsupported set_name target '${declaration.target}'")
        }
    }
}
