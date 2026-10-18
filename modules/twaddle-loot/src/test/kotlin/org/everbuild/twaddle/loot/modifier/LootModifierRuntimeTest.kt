package org.everbuild.twaddle.loot.modifier

import com.google.gson.JsonParser
import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.Result
import net.minestom.server.codec.Transcoder
import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.Material
import net.minestom.server.item.component.PotionContents
import net.minestom.server.potion.PotionType
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.withExplosionRadius
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.core.test.envTest
import org.everbuild.twaddle.loot.modifier.impl.DiscardLootModifier
import org.everbuild.twaddle.loot.modifier.impl.ExplosionDecayLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetCountLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetDamageLootModifier
import org.everbuild.twaddle.loot.modifier.impl.SetPotionLootModifier
import java.util.Random
import kotlin.math.floor

private val datapackPrimitives = DatapackPrimitives.vanilla()
private val modifierTypes = createVanillaLootModifierTypeRegistry()

private fun decodeProvider(codec: Codec<NumberProviderDeclaration<Int>>, json: String): NumberProviderDeclaration<Int> =
    when (val result = codec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> result.value
        is Result.Error -> error("Could not decode integer number provider: ${result.message}")
    }

private fun decodeFloatProvider(json: String): NumberProviderDeclaration<Float> =
    when (val result = datapackPrimitives.floatNumberProviderCodec.decode(Transcoder.JSON, JsonParser.parseString(json))) {
        is Result.Ok -> result.value
        is Result.Error -> error("Could not decode float number provider: ${result.message}")
    }

private fun runtimeContext() = DataContext(hashMapOf())

private fun apply(modifier: LootModifier, item: ItemStack, context: DataContext = runtimeContext(), random: Random = Random(0)) =
    applyLootModifier(modifier, item, modifierTypes, context, random)

val lootModifierRuntimeTests by testSuite("Loot modifier runtime") {
    test("set_count evaluates the regular integer provider and replaces or adds") {
        val provider = decodeProvider(datapackPrimitives.intNumberProviderCodec, "4")
        val item = ItemStack.of(Material.ARROW, 3)

        apply(SetCountLootModifier(provider), item)!!.amount() shouldBe 4
        apply(SetCountLootModifier(provider, add = true), item)!!.amount() shouldBe 7
    }

    test("set_count discards a stack when the resulting count is zero") {
        val provider = decodeProvider(datapackPrimitives.intNumberProviderCodec, "0")

        apply(SetCountLootModifier(provider), ItemStack.of(Material.STICK)) shouldBe ItemStack.AIR
    }

    envTest("set_potion writes the resolved potion contents") { _ ->
        val potion = PotionType.fromKey(Key.key("minecraft:healing"))

        val result = apply(SetPotionLootModifier(Key.key("minecraft:healing")), ItemStack.of(Material.POTION))

        result!!.get(DataComponents.POTION_CONTENTS) shouldBe PotionContents(potion)
    }

    test("discard removes the item") {
        apply(DiscardLootModifier, ItemStack.of(Material.STICK)) shouldBe null
    }

    test("explosion_decay uses the shared explosion-radius context and random source") {
        val item = ItemStack.of(Material.STICK, 5)
        val context = runtimeContext().withExplosionRadius(2f)
        val expectedRandom = Random(42)
        val expectedSurvivors = (0 until item.amount()).count { expectedRandom.nextFloat() <= 0.5f }

        apply(ExplosionDecayLootModifier, item, context, Random(42))!!.amount() shouldBe expectedSurvivors
    }

    test("set_damage evaluates the regular float provider") {
        val provider = decodeFloatProvider("0.8")
        val item = ItemStack.of(Material.DIAMOND_SWORD)
        val maxDamage = item.get(DataComponents.MAX_DAMAGE)!!
        val expected = floor(maxDamage * (1f - 0.8f)).toInt()

        apply(SetDamageLootModifier(provider), item)!!.get(DataComponents.DAMAGE) shouldBe expected

        val initiallyDamaged = item.with(DataComponents.DAMAGE, maxDamage / 4)
        val additionalDamage = decodeFloatProvider("0.1")
        val base = 1f - (maxDamage / 4).toFloat() / maxDamage
        val expectedAfterAddition = floor(maxDamage * (1f - (0.1f + base).coerceIn(0f, 1f))).toInt()

        apply(SetDamageLootModifier(additionalDamage, add = true), initiallyDamaged)
            ?.get(DataComponents.DAMAGE) shouldBe expectedAfterAddition
    }
}
