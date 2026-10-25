package org.everbuild.twaddle.loot.modifier.impl

import java.util.Random
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponent
import net.minestom.server.component.DataComponentMap
import net.minestom.server.item.ItemStack
import net.minestom.server.registry.RegistryTranscoder
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.loot.modifier.context.LootModifierRegistriesContext

data class SetComponentsLootModifier(val components: Codec.RawValue) : LootModifier {
    override fun type() = LootModifierKeys.SET_COMPONENTS
}

internal class SetComponentsLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct("components", Codec.RAW_VALUE, { (it as SetComponentsLootModifier).components }, ::SetComponentsLootModifier)!!

    @Suppress("UnstableApiUsage")
    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val registries = context[LootModifierRegistriesContext]
        val transcoder = RegistryTranscoder(Transcoder.JSON, registries)
        val rawComponents = (modifier as SetComponentsLootModifier).components
        val encodedComponents = rawComponents.convertTo(Transcoder.JSON).orElseThrow()
        val patch = DataComponent.PATCH_CODEC.decode(transcoder, encodedComponents).orElseThrow()
        val mergedPatch = DataComponentMap.applyPatch(item.componentPatch(), patch)
        return ItemStack.of(item.material(), item.amount(), mergedPatch)
    }
}
