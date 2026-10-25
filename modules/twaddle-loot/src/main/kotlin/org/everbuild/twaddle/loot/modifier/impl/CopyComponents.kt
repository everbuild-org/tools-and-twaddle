package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.component.DataComponent
import net.minestom.server.item.ItemStack
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.loot.modifier.context.LootComponentSourceAccessContext

data class CopyComponentsLootModifier(val source: String, val include: List<Key>?, val exclude: List<Key>?) : LootModifier {
    override fun type() = LootModifierKeys.COPY_COMPONENTS
}

internal class CopyComponentsLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("source", Codec.STRING, { (it as CopyComponentsLootModifier).source }, "include", Codec.KEY.list().optional(), { (it as CopyComponentsLootModifier).include }, "exclude", Codec.KEY.list().optional(), { (it as CopyComponentsLootModifier).exclude }, ::CopyComponentsLootModifier)!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val declaration = modifier as CopyComponentsLootModifier
        val sourceComponents = context[LootComponentSourceAccessContext].getComponents(declaration.source) ?: return item
        val included = declaration.include?.toSet()
        val excluded = declaration.exclude.orEmpty().toSet()

        var result = item
        for (entry in sourceComponents.entrySet()) {
            val component = entry.component()
            if (included != null && component.key() !in included) continue
            if (component.key() in excluded) continue
            val value = entry.value() ?: continue
            result = result.withComponent(component, value)
        }
        return result
    }

    @Suppress("UNCHECKED_CAST")
    private fun ItemStack.withComponent(component: DataComponent<*>, value: Any): ItemStack =
        with(component as DataComponent<Any>, value)
}
