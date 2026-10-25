package org.everbuild.twaddle.loot.modifier

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.instance.block.Block
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.ItemBlockState
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.context.withItemPredicateMatcher
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.loot.modifier.impl.CopyStateLootModifier
import org.everbuild.twaddle.loot.modifier.impl.DiscardLootModifier
import org.everbuild.twaddle.loot.modifier.impl.ExplorationMapLootModifier
import org.everbuild.twaddle.loot.modifier.impl.FilteredLootModifier
import org.everbuild.twaddle.loot.modifier.impl.FurnaceSmeltLootModifier
import java.util.Random
import com.google.gson.JsonParser

private fun emptyContext() = DataContext(hashMapOf())

private object AlwaysPassPredicate : ContextPredicate {
    override fun type() = Key.key("loot_test:always_pass")
    override fun test(context: DataContext, random: Random) = true
}

private object AlwaysFailPredicate : ContextPredicate {
    override fun type() = Key.key("loot_test:always_fail")
    override fun test(context: DataContext, random: Random) = false
}

private fun apply(modifier: LootModifier, item: ItemStack, context: DataContext) =
    applyLootModifier(modifier, item, createVanillaLootModifierTypeRegistry(), context, Random(0))

val lootModifierWorldRuntimeTests by testSuite("Loot modifier world runtime") {
    envTest("copy_state copies only requested properties when the source block matches") { _ ->
        val source = requireNotNull(Block.fromKey(Key.key("minecraft:oak_log"))).withProperty("axis", "x")
        val item = ItemStack.of(Material.OAK_LOG).with(
            DataComponents.BLOCK_STATE,
            ItemBlockState(mapOf("waterlogged" to "true", "custom" to "kept")),
        )
        val context = emptyContext().withLootBlockStateAccess { source }

        val result = apply(
            CopyStateLootModifier(Key.key("minecraft:oak_log"), listOf("axis")),
            item,
            context,
        )!!

        result.get(DataComponents.BLOCK_STATE) shouldBe ItemBlockState(
            mapOf("axis" to "x", "waterlogged" to "true", "custom" to "kept"),
        )
    }

    envTest("copy_state leaves the item unchanged when the source block does not match") { _ ->
        val source = requireNotNull(Block.fromKey(Key.key("minecraft:oak_log"))).withProperty("axis", "x")
        val item = ItemStack.of(Material.OAK_LOG)
        val context = emptyContext().withLootBlockStateAccess { source }

        apply(CopyStateLootModifier(Key.key("minecraft:birch_log"), listOf("axis")), item, context) shouldBe item
    }

    test("furnace_smelt replaces the material while preserving the input count") {
        val input = ItemStack.of(Material.RAW_IRON, 3)
        val context = emptyContext().withFurnaceSmeltAccess { item ->
            if (item.material() == Material.RAW_IRON) ItemStack.of(Material.IRON_INGOT) else null
        }

        val result = apply(FurnaceSmeltLootModifier, input, context)

        result!!.material() shouldBe Material.IRON_INGOT
        result.amount() shouldBe 3
    }

    test("furnace_smelt leaves an item unchanged when no recipe matches") {
        val input = ItemStack.of(Material.STICK, 3)
        val context = emptyContext().withFurnaceSmeltAccess { null }

        apply(FurnaceSmeltLootModifier, input, context) shouldBe input
    }

    test("furnace_smelt skips recipe lookup when its condition fails") {
        val input = ItemStack.of(Material.RAW_IRON, 3)

        apply(LootModifierWithConditions(FurnaceSmeltLootModifier, listOf(AlwaysFailPredicate)), input, emptyContext()) shouldBe input
    }

    test("exploration_map passes lookup settings to the host and preserves stack count") {
        val input = ItemStack.of(Material.MAP, 2)
        val request = ExplorationMapRequest(
            destination = "#minecraft:on_treasure_maps",
            decoration = "minecraft:red_x",
            zoom = 1,
            searchRadius = 12,
            skipExistingChunks = false,
        )
        var received: ExplorationMapRequest? = null
        val context = emptyContext().withExplorationMapAccess { item, lookup, _ ->
            received = lookup
            item.with(DataComponents.MAP_ID, 17)
        }
        val modifier = ExplorationMapLootModifier(
            decoration = request.decoration,
            destination = request.destination,
            zoom = request.zoom,
            searchRadius = request.searchRadius,
            skipExistingChunks = request.skipExistingChunks,
        )

        val result = apply(modifier, input, context)!!

        received shouldBe request
        result.get(DataComponents.MAP_ID) shouldBe 17
        result.amount() shouldBe 2
    }

    test("exploration_map leaves the item unchanged when no destination is found") {
        val input = ItemStack.of(Material.MAP)
        val context = emptyContext().withExplorationMapAccess { _, _, _ -> null }

        apply(ExplorationMapLootModifier(), input, context) shouldBe input
    }

    test("filtered keeps a matching item and applies on_fail through the active modifier registry") {
        val input = ItemStack.of(Material.STICK)
        val itemFilter = Codec.RAW_VALUE
        val rawPredicate = when (val result = itemFilter.decode(Transcoder.JSON, JsonParser.parseString("{}"))) {
            is net.minestom.server.codec.Result.Ok -> result.value
            is net.minestom.server.codec.Result.Error -> error(result.message)
        }
        val registry = createVanillaLootModifierTypeRegistry()
        val matchingContext = emptyContext()
            .withItemPredicateMatcher { item, _ -> item.material() == Material.STICK }
        val failingContext = emptyContext()
            .withItemPredicateMatcher { _, _ -> false }
        val modifier = FilteredLootModifier(rawPredicate, DiscardLootModifier)

        applyLootModifier(modifier, input, registry, matchingContext, Random(0)) shouldBe input
        applyLootModifier(modifier, input, registry, failingContext, Random(0)) shouldBe null
    }

    test("filtered applies on_pass when its item filter matches") {
        val input = ItemStack.of(Material.STICK)
        val rawPredicate = when (val result = Codec.RAW_VALUE.decode(Transcoder.JSON, JsonParser.parseString("{}"))) {
            is net.minestom.server.codec.Result.Ok -> result.value
            is net.minestom.server.codec.Result.Error -> error(result.message)
        }
        val registry = createVanillaLootModifierTypeRegistry()
        val matchingContext = emptyContext()
            .withItemPredicateMatcher { item, _ -> item.material() == Material.STICK }
        val failingContext = emptyContext()
            .withItemPredicateMatcher { _, _ -> false }
        val modifier = FilteredLootModifier(rawPredicate, onPass = listOf(DiscardLootModifier))

        applyLootModifier(modifier, input, registry, matchingContext, Random(0)) shouldBe null
        applyLootModifier(modifier, input, registry, failingContext, Random(0)) shouldBe input
    }
}
