package org.everbuild.twaddle.loot.modifier

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponentMap
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.component.SuspiciousStewEffects
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.potion.PotionEffect
import net.minestom.server.registry.RegistryKey
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.loot.modifier.context.withLootAdditionalTradeCostAllowed
import org.everbuild.twaddle.loot.modifier.context.withLootComponentSourceAccess
import org.everbuild.twaddle.loot.modifier.context.withLootModifierRegistries
import org.everbuild.twaddle.loot.modifier.impl.CopyComponentsLootModifier
import org.everbuild.twaddle.loot.modifier.impl.EnchantRandomlyLootModifier
import org.everbuild.twaddle.loot.modifier.impl.EnchantWithLevelsLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetComponentsLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetEnchantmentsLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetInstrumentLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetNameLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetStewEffectLootModifier
import org.everbuild.twaddle.loot.modifier.impl.StewEffect
import java.util.Random

private val numberProviders = DatapackPrimitives.vanilla()
private val lootModifierTypes = createVanillaLootModifierTypeRegistry()
private val sharpness = Key.key("minecraft:sharpness")
private val sharpnessKey = RegistryKey.of<Enchantment>(sharpness)

private fun context() = DataContext(hashMapOf())

private fun apply(
    modifier: LootModifier,
    item: ItemStack,
    context: DataContext,
    random: Random = Random(0),
) = checkNotNull(applyLootModifier(modifier, item, lootModifierTypes, context, random))

private fun decodeIntProvider(json: String): NumberProviderDeclaration<Int> =
    when (val result = numberProviders.intNumberProviderCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> result.value
        is Result.Error -> error("Could not decode integer number provider: ${result.message}")
    }

private fun decodeRaw(json: String): Codec.RawValue =
    when (val result = Codec.RAW_VALUE.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> result.value
        is Result.Error -> error("Could not decode raw JSON: ${result.message}")
    }

val componentLootModifierRuntimeTests by testSuite("Loot component modifier runtime") {
    envTest("set_components applies a registry-aware component patch") { env ->
        val context = context().withLootModifierRegistries(env.process().registries())
        val result = apply(
            SetComponentsLootModifier(decodeRaw("""{"minecraft:custom_name":{"text":"patched"}}""")),
            ItemStack.of(Material.STICK),
            context,
        )

        result.get(DataComponents.CUSTOM_NAME) shouldBe Component.text("patched")
    }

    test("copy_components copies selected data and honors exclusions") {
        val source = DataComponentMap.builder()
            .set(DataComponents.CUSTOM_NAME, Component.text("copied custom name"))
            .set(DataComponents.ITEM_NAME, Component.text("excluded item name"))
            .build()
        val context = context().withLootComponentSourceAccess { sourceName ->
            if (sourceName == "block_entity") source else null
        }
        val modifier = CopyComponentsLootModifier(
            source = "block_entity",
            include = listOf(Key.key("minecraft:custom_name"), Key.key("minecraft:item_name")),
            exclude = listOf(Key.key("minecraft:item_name")),
        )

        val result = apply(modifier, ItemStack.of(Material.BARREL), context)

        result.get(DataComponents.CUSTOM_NAME) shouldBe Component.text("copied custom name")
        result.get(DataComponents.ITEM_NAME) shouldNotBe Component.text("excluded item name")
    }

    test("set_name selects the custom_name or item_name component") {
        val item = ItemStack.of(Material.STICK)

        val customName = apply(SetNameLootModifier(Component.text("custom")), item, context())
        val itemName = apply(SetNameLootModifier(Component.text("item"), "item_name"), item, context())

        customName.get(DataComponents.CUSTOM_NAME) shouldBe Component.text("custom")
        itemName.get(DataComponents.ITEM_NAME) shouldBe Component.text("item")
    }

    envTest("set_instrument resolves a direct registry key") { env ->
        val context = context().withLootModifierRegistries(env.process().registries())
        val result = apply(
            SetInstrumentLootModifier("minecraft:ponder_goat_horn"),
            ItemStack.of(Material.GOAT_HORN),
            context,
        )

        result.get(DataComponents.INSTRUMENT) shouldBe RegistryKey.of(Key.key("minecraft:ponder_goat_horn"))
    }

    test("set_stew_effect selects one effect and converts seconds to ticks") {
        val effect = checkNotNull(PotionEffect.fromKey(Key.key("minecraft:night_vision")))
        val modifier = SetStewEffectLootModifier(
            listOf(StewEffect(Key.key("minecraft:night_vision"), decodeIntProvider("5"))),
        )

        val result = apply(modifier, ItemStack.of(Material.SUSPICIOUS_STEW), context())
        val effectResult = checkNotNull(result.get(DataComponents.SUSPICIOUS_STEW_EFFECTS)).effects().single()

        effectResult shouldBe SuspiciousStewEffects.Effect(effect, 100)
        apply(modifier, ItemStack.of(Material.STICK), context()) shouldBe ItemStack.of(Material.STICK)
    }

    envTest("set_enchantments evaluates levels, adds to current levels, and stores book enchants") { env ->
        val context = context().withLootModifierRegistries(env.process().registries())
        val three = decodeIntProvider("3")
        val sword = ItemStack.of(Material.DIAMOND_SWORD)
            .with(DataComponents.ENCHANTMENTS, EnchantmentList(sharpnessKey, 2))
        val added = apply(SetEnchantmentsLootModifier(mapOf(sharpness to three), add = true), sword, context)
        val book = apply(
            SetEnchantmentsLootModifier(mapOf(sharpness to three)),
            ItemStack.of(Material.BOOK),
            context,
        )

        added.get(DataComponents.ENCHANTMENTS)?.level(sharpnessKey) shouldBe 5
        book.material() shouldBe Material.ENCHANTED_BOOK
        book.get(DataComponents.STORED_ENCHANTMENTS)?.level(sharpnessKey) shouldBe 3
    }

    envTest("enchant_randomly selects a compatible enchantment and converts books") { env ->
        val context = context()
            .withLootModifierRegistries(env.process().registries())
            .withLootAdditionalTradeCostAllowed(true)
        val modifier = EnchantRandomlyLootModifier(
            options = "minecraft:sharpness",
            includeAdditionalCostComponent = true,
        )

        val sword = apply(modifier, ItemStack.of(Material.DIAMOND_SWORD), context, Random(41))
        val book = apply(
            EnchantRandomlyLootModifier(options = "minecraft:sharpness"),
            ItemStack.of(Material.BOOK),
            context,
            Random(41),
        )

        val swordLevel = checkNotNull(sword.get(DataComponents.ENCHANTMENTS)).level(sharpnessKey)
        (1..5).contains(swordLevel) shouldBe true
        sword.get(DataComponents.ADDITIONAL_TRADE_COST) shouldNotBe null
        book.material() shouldBe Material.ENCHANTED_BOOK
        book.get(DataComponents.STORED_ENCHANTMENTS)?.level(sharpnessKey) shouldNotBe 0

        val stick = ItemStack.of(Material.STICK)
        apply(EnchantRandomlyLootModifier(options = "minecraft:sharpness"), stick, context) shouldBe stick
        val incompatibleAllowed = apply(
            EnchantRandomlyLootModifier(options = "minecraft:sharpness", onlyCompatible = false),
            stick,
            context,
        )
        incompatibleAllowed.get(DataComponents.ENCHANTMENTS)?.level(sharpnessKey) shouldNotBe 0

        val taggedBook = apply(
            EnchantRandomlyLootModifier(options = "#minecraft:exclusive_set/armor"),
            ItemStack.of(Material.BOOK),
            context,
            Random(42),
        )
        taggedBook.material() shouldBe Material.ENCHANTED_BOOK
        taggedBook.get(DataComponents.STORED_ENCHANTMENTS) shouldNotBe null
    }

    envTest("enchant_with_levels evaluates levels, applies an option, and gates the extra cost component") { env ->
        val context = context()
            .withLootModifierRegistries(env.process().registries())
            .withLootAdditionalTradeCostAllowed(true)
        val modifier = EnchantWithLevelsLootModifier(
            levels = decodeIntProvider("30"),
            options = "minecraft:sharpness",
            includeAdditionalCostComponent = true,
        )

        val result = apply(modifier, ItemStack.of(Material.DIAMOND_SWORD), context, Random(23))
        val enchantments = checkNotNull(result.get(DataComponents.ENCHANTMENTS))

        enchantments.level(sharpnessKey) shouldNotBe 0
        result.get(DataComponents.ADDITIONAL_TRADE_COST) shouldBe 30
    }

    envTest("enchant_with_levels handles items without eligible enchantments and converts books") { env ->
        val context = context().withLootModifierRegistries(env.process().registries())
        val levels = decodeIntProvider("30")
        val modifier = EnchantWithLevelsLootModifier(levels, options = "minecraft:sharpness")

        val stick = ItemStack.of(Material.STICK)
        apply(modifier, stick, context) shouldBe stick

        val disabledSword = ItemStack.of(Material.DIAMOND_SWORD).with(DataComponents.ENCHANTABLE, 0)
        apply(modifier, disabledSword, context) shouldBe disabledSword

        val ineligibleItem = ItemStack.of(Material.STICK).with(DataComponents.ENCHANTABLE, 10)
        apply(modifier, ineligibleItem, context) shouldBe ineligibleItem

        val book = apply(modifier, ItemStack.of(Material.BOOK), context, Random(24))
        book.material() shouldBe Material.ENCHANTED_BOOK
        book.get(DataComponents.STORED_ENCHANTMENTS)?.level(sharpnessKey) shouldNotBe 0

        val unrestricted = apply(
            EnchantWithLevelsLootModifier(levels = decodeIntProvider("60")),
            ItemStack.of(Material.DIAMOND_SWORD),
            context,
            Random(25),
        )
        unrestricted.get(DataComponents.ENCHANTMENTS) shouldNotBe null

        val exclusiveSet = apply(
            EnchantWithLevelsLootModifier(
                levels = decodeIntProvider("30"),
                options = "#minecraft:exclusive_set/damage",
            ),
            ItemStack.of(Material.DIAMOND_SWORD),
            context,
            Random(27),
        )
        exclusiveSet.get(DataComponents.ENCHANTMENTS) shouldNotBe null

        shouldThrow<IllegalStateException> {
            apply(modifier.copy(options = "#minecraft:missing_enchantments"), ItemStack.of(Material.DIAMOND_SWORD), context)
        }
        shouldThrow<IllegalStateException> {
            apply(modifier.copy(options = "minecraft:missing_enchantment"), ItemStack.of(Material.DIAMOND_SWORD), context)
        }

        val noCostContext = context.withLootAdditionalTradeCostAllowed(false)
        val noCostModifier = modifier.copy(includeAdditionalCostComponent = true)
        val noCostResult = apply(noCostModifier, ItemStack.of(Material.DIAMOND_SWORD), noCostContext, Random(26))
        noCostResult.get(DataComponents.ADDITIONAL_TRADE_COST) shouldBe null
    }
}
