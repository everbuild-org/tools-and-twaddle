package org.everbuild.twaddle.loot.modifier

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.EnchantmentList
import net.minestom.server.item.enchant.Enchantment
import net.minestom.server.registry.RegistryKey
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.withEnchantment
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.loot.modifier.impl.ApplyBonusLootModifier
import org.everbuild.twaddle.loot.modifier.impl.ApplyBonusParameters
import org.everbuild.twaddle.loot.modifier.impl.EnchantedCountIncreaseLootModifier
import org.everbuild.twaddle.loot.modifier.impl.ItemCountLimit
import org.everbuild.twaddle.loot.modifier.impl.LimitCountLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetOminousBottleAmplifierLootModifier
import java.util.Random

private val primitives = DatapackPrimitives.vanilla()
private val fortune = Key.key("minecraft:fortune")

private fun enchantments(level: Int) = EnchantmentList(
    linkedMapOf(RegistryKey.of<Enchantment>(fortune) to level),
)

private fun modifierContext(enchantmentLevel: Int = 0) = DataContext(hashMapOf())
    .withEnchantment(if (enchantmentLevel == 0) null else enchantments(enchantmentLevel))

private fun <T> decodeProvider(codec: Codec<NumberProviderDeclaration<T>>, json: String): NumberProviderDeclaration<T> =
    when (val result = codec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> result.value
        is Result.Error -> error("Could not decode number provider: ${result.message}")
    }

private fun apply(
    modifier: LootModifier,
    item: ItemStack,
    context: DataContext = modifierContext(),
    random: Random = Random(0),
) = applyLootModifier(modifier, item, createVanillaLootModifierTypeRegistry(), context, random)

val countLootModifierRuntimeTests by testSuite("Loot count modifier runtime") {
    test("apply_bonus binomial formula adds the seeded number of successes") {
        val modifier = ApplyBonusLootModifier(
            fortune,
            "minecraft:binomial_with_bonus_count",
            ApplyBonusParameters(extra = 3, probability = 0.57f),
        )
        val expectedRandom = Random(41)
        val expectedBonus = (0 until 5).count { expectedRandom.nextFloat() < 0.57f }

        apply(modifier, ItemStack.of(Material.BEETROOT_SEEDS, 2), modifierContext(enchantmentLevel = 2), Random(41))
            ?.amount() shouldBe 2 + expectedBonus
    }

    test("apply_bonus uniform formula adds a value from zero through level times multiplier") {
        val modifier = ApplyBonusLootModifier(
            fortune,
            "minecraft:uniform_bonus_count",
            ApplyBonusParameters(bonusMultiplier = 2),
        )
        val expectedBonus = Random(9).nextInt(7)

        apply(modifier, ItemStack.of(Material.MELON_SLICE, 3), modifierContext(enchantmentLevel = 3), Random(9))
            ?.amount() shouldBe 3 + expectedBonus
    }

    test("apply_bonus ore formula multiplies by at least one") {
        val modifier = ApplyBonusLootModifier(fortune, "minecraft:ore_drops")
        val expectedMultiplier = maxOf(1, Random(12).nextInt(5))

        apply(modifier, ItemStack.of(Material.DIAMOND, 2), modifierContext(enchantmentLevel = 3), Random(12))
            ?.amount() shouldBe 2 * expectedMultiplier
    }

    test("apply_bonus leaves the stack and random stream unchanged without the enchantment") {
        val modifier = ApplyBonusLootModifier(fortune, "minecraft:ore_drops")
        val random = Random(15)
        val expectedNext = Random(15).nextInt()
        val item = ItemStack.of(Material.DIAMOND, 2)

        apply(modifier, item, modifierContext(), random) shouldBe item
        random.nextInt() shouldBe expectedNext
    }

    test("enchanted_count_increase floors the per-level bonus and applies its optional cap") {
        val count = decodeProvider(primitives.floatNumberProviderCodec, "0.4")
        val item = ItemStack.of(Material.BLAZE_ROD, 2)

        apply(EnchantedCountIncreaseLootModifier(count, fortune), item, modifierContext(enchantmentLevel = 3))
            ?.amount() shouldBe 3
        apply(EnchantedCountIncreaseLootModifier(count, fortune, limit = 2), item, modifierContext(enchantmentLevel = 3))
            ?.amount() shouldBe 2
    }

    test("limit_count supports exact and provider-backed range limits") {
        val item = ItemStack.of(Material.STICK, 6)
        val exact = LimitCountLootModifier(ItemCountLimit.Exact(4))
        val range = LimitCountLootModifier(
            ItemCountLimit.Range(
                min = decodeProvider(primitives.intNumberProviderCodec, "2"),
                max = decodeProvider(primitives.intNumberProviderCodec, "5"),
            ),
        )

        apply(exact, item)?.amount() shouldBe 4
        apply(range, item)?.amount() shouldBe 5
        apply(range, ItemStack.of(Material.STICK))?.amount() shouldBe 2
    }

    test("limit_count rejects a range whose evaluated minimum exceeds its maximum") {
        val range = LimitCountLootModifier(
            ItemCountLimit.Range(
                min = decodeProvider(primitives.intNumberProviderCodec, "5"),
                max = decodeProvider(primitives.intNumberProviderCodec, "2"),
            ),
        )

        shouldThrow<IllegalArgumentException> { apply(range, ItemStack.of(Material.STICK)) }
    }

    envTest("set_ominous_bottle_amplifier evaluates its integer provider and writes the component") { _ ->
        val amplifier = decodeProvider(
            primitives.intNumberProviderCodec,
            """{"type":"minecraft:uniform","min":0,"max":4}""",
        )
        val expected = amplifier.get(modifierContext(), Random(27))

        apply(
            SetOminousBottleAmplifierLootModifier(amplifier),
            ItemStack.of(Material.OMINOUS_BOTTLE),
            random = Random(27),
        )?.get(DataComponents.OMINOUS_BOTTLE_AMPLIFIER) shouldBe expected
    }
}
