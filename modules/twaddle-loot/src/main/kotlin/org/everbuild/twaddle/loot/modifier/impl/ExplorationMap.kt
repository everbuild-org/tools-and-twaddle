package org.everbuild.twaddle.loot.modifier.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.loot.modifier.LootModifier
import org.everbuild.twaddle.loot.modifier.LootModifierKeys
import org.everbuild.twaddle.loot.modifier.LootModifierType
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.loot.modifier.ExplorationMapContext
import org.everbuild.twaddle.loot.modifier.ExplorationMapRequest
import net.minestom.server.item.ItemStack
import java.util.Random

data class ExplorationMapLootModifier(
    val decoration: String = "mansion",
    val destination: String = "#minecraft:on_treasure_maps",
    val zoom: Int = 2,
    val searchRadius: Int = 50,
    val skipExistingChunks: Boolean = true,
) : LootModifier {
    override fun type() = LootModifierKeys.EXPLORATION_MAP
}

internal class ExplorationMapLootModifierType : LootModifierType {
    override fun codec(modifierCodec: Codec<LootModifier>, predicateCodec: Codec<ContextPredicate>, intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>, floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>) =
        StructCodec.struct(
            "decoration", Codec.STRING.optional("mansion"), { (it as ExplorationMapLootModifier).decoration },
            "destination", Codec.STRING.optional("#minecraft:on_treasure_maps"), { (it as ExplorationMapLootModifier).destination },
            "zoom", Codec.INT.optional(2), { (it as ExplorationMapLootModifier).zoom },
            "search_radius", Codec.INT.optional(50), { (it as ExplorationMapLootModifier).searchRadius },
            "skip_existing_chunks", Codec.BOOLEAN.optional(true), { (it as ExplorationMapLootModifier).skipExistingChunks },
            ::ExplorationMapLootModifier,
        )!!

    override fun apply(modifier: LootModifier, item: ItemStack, context: DataContext, random: Random): ItemStack {
        val mapModifier = modifier as ExplorationMapLootModifier
        val request = ExplorationMapRequest(
            mapModifier.destination,
            mapModifier.decoration,
            mapModifier.zoom,
            mapModifier.searchRadius,
            mapModifier.skipExistingChunks,
        )
        val map = context[ExplorationMapContext].createMap(item, request, context) ?: return item
        return map.withAmount(item.amount())
    }
}
