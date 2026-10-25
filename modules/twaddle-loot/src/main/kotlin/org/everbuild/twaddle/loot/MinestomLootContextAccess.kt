package org.everbuild.twaddle.loot

import com.google.gson.JsonObject
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.component.DataComponent
import net.minestom.server.component.DataComponentMap
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.predicate.BlockPredicate as MinestomBlockPredicate
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.predicate.ItemPredicate
import net.minestom.server.registry.Registries
import net.minestom.server.registry.RegistryTranscoder
import org.everbuild.twaddle.datapack_primitives.context.BlockPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.ItemPredicateMatcher
import org.everbuild.twaddle.datapack_primitives.context.LocationContext
import org.everbuild.twaddle.datapack_primitives.context.ToolPredicateMatcher
import org.everbuild.twaddle.loot.modifier.context.LootComponentSourceAccess

internal class MinestomLootContextAccess(
    val instance: Instance,
    val registries: Registries,
    val block: Block?,
    val tool: ItemStack?,
    val origin: ContextLocation,
    val entityTargets: Map<String, net.minestom.server.entity.Entity>,
    val structureMatcher: StructurePredicateMatcher?,
) {
    private val blockEntityComponents by lazy { block?.let { decodeBlockEntityComponents(it, registries) } }

    val componentSourceAccess = LootComponentSourceAccess { source ->
        when (source) {
            "block_entity" -> blockEntityComponents
            else -> null
        }
    }

    val dynamicLootResolver = DynamicLootResolver { name, _, _ ->
        when (name.asString()) {
            "minecraft:contents" -> blockEntityComponents
                ?.get(DataComponents.CONTAINER)
                .orEmpty()
            "minecraft:sherds" -> blockEntityComponents
                ?.get(DataComponents.POT_DECORATIONS)
                ?.let { decorations ->
                    listOfNotNull(
                        decorations.back(),
                        decorations.left(),
                        decorations.right(),
                        decorations.front(),
                    ).filter { it.material() != Material.BRICK }
                }
                .orEmpty()
            else -> error("No vanilla dynamic loot source for '$name'")
        }
    }

    val itemPredicateMatcher = ItemPredicateMatcher { item, predicate ->
        decodeItemPredicate(predicate, registries).test(item)
    }

    val toolPredicateMatcher: ToolPredicateMatcher? = tool?.let { item ->
        ToolPredicateMatcher { predicate -> decodeItemPredicate(predicate, registries).test(item) }
    }

    val blockPredicateMatcher: BlockPredicateMatcher? = block?.let { currentBlock ->
        BlockPredicateMatcher { fields -> decodeBlockPredicate(fields, registries).test(currentBlock) }
    }

    private val locationPredicateEvaluator =
        VanillaLocationPredicateEvaluator(instance, registries, structureMatcher)

    val locationContext = LocationContext(
        origin = origin,
        evaluator = locationPredicateEvaluator::matches,
    )

    val entityPredicateMatcher: EntityPredicateMatcher = MinestomEntityPredicateMatcher(
        entities = entityTargets,
        origin = origin,
        registries = registries,
        locationContext = locationContext,
        itemPredicateMatcher = itemPredicateMatcher,
    )
}

private fun decodeItemPredicate(predicate: Codec.RawValue, registries: Registries): ItemPredicate {
    val json = predicate.convertTo(Transcoder.JSON).orElseThrow()
    return ItemPredicate.CODEC
        .decode(RegistryTranscoder(Transcoder.JSON, registries), json)
        .orElseThrow()
}

private fun decodeBlockPredicate(
    fields: Map<String, Codec.RawValue>,
    registries: Registries,
): MinestomBlockPredicate {
    val json = JsonObject()
    for ((name, rawValue) in fields) {
        json.add(name, rawValue.convertTo(Transcoder.JSON).orElseThrow())
    }
    return MinestomBlockPredicate.CODEC
        .decode(RegistryTranscoder(Transcoder.JSON, registries), json)
        .orElseThrow()
}

private fun decodeBlockEntityComponents(block: Block, registries: Registries): DataComponentMap? {
    val nbt = block.nbt() ?: return null
    val components = nbt.getCompound("components")
    return when (val decoded = DataComponent.MAP_NBT_TYPE.decode(RegistryTranscoder(Transcoder.NBT, registries), components)) {
        is Result.Ok -> decoded.value
        is Result.Error -> error("Could not decode block entity components: $decoded")
    }
}
