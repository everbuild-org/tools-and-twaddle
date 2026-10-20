package org.everbuild.twaddle.loot.modifier

import net.kyori.adventure.key.Key
import net.minestom.server.registry.DynamicRegistry
import org.everbuild.twaddle.loot.modifier.impl.ApplyBonusLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.CopyComponentsLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.CopyStateLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.DiscardLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.EnchantRandomlyLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.EnchantWithLevelsLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.EnchantedCountIncreaseLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.ExplorationMapLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.ExplosionDecayLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.FilteredLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.FurnaceSmeltLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.LimitCountLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetComponentsLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetCountLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetDamageLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetEnchantmentsLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetInstrumentLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetNameLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetOminousBottleAmplifierLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetPotionLootModifierType
import org.everbuild.twaddle.loot.modifier.impl.SetStewEffectLootModifierType

internal object LootModifierKeys {
    val SET_COUNT = Key.key("minecraft:set_count")
    val SET_POTION = Key.key("minecraft:set_potion")
    val APPLY_BONUS = Key.key("minecraft:apply_bonus")
    val COPY_COMPONENTS = Key.key("minecraft:copy_components")
    val COPY_STATE = Key.key("minecraft:copy_state")
    val ENCHANT_RANDOMLY = Key.key("minecraft:enchant_randomly")
    val ENCHANT_WITH_LEVELS = Key.key("minecraft:enchant_with_levels")
    val ENCHANTED_COUNT_INCREASE = Key.key("minecraft:enchanted_count_increase")
    val EXPLORATION_MAP = Key.key("minecraft:exploration_map")
    val EXPLOSION_DECAY = Key.key("minecraft:explosion_decay")
    val FILTERED = Key.key("minecraft:filtered")
    val FURNACE_SMELT = Key.key("minecraft:furnace_smelt")
    val LIMIT_COUNT = Key.key("minecraft:limit_count")
    val SET_COMPONENTS = Key.key("minecraft:set_components")
    val SET_DAMAGE = Key.key("minecraft:set_damage")
    val SET_ENCHANTMENTS = Key.key("minecraft:set_enchantments")
    val SET_INSTRUMENT = Key.key("minecraft:set_instrument")
    val SET_NAME = Key.key("minecraft:set_name")
    val SET_OMINOUS_BOTTLE_AMPLIFIER = Key.key("minecraft:set_ominous_bottle_amplifier")
    val SET_STEW_EFFECT = Key.key("minecraft:set_stew_effect")
    val DISCARD = Key.key("minecraft:discard")
}

/** Registers the vanilla loot-modifier types currently implemented by this module. */
fun addAllVanillaLootModifierTypes(registry: DynamicRegistry<LootModifierType>) {
    registry.register(LootModifierKeys.SET_COUNT, SetCountLootModifierType())
    registry.register(LootModifierKeys.SET_POTION, SetPotionLootModifierType())
    registry.register(LootModifierKeys.APPLY_BONUS, ApplyBonusLootModifierType())
    registry.register(LootModifierKeys.COPY_COMPONENTS, CopyComponentsLootModifierType())
    registry.register(LootModifierKeys.COPY_STATE, CopyStateLootModifierType())
    registry.register(LootModifierKeys.ENCHANT_RANDOMLY, EnchantRandomlyLootModifierType())
    registry.register(LootModifierKeys.ENCHANT_WITH_LEVELS, EnchantWithLevelsLootModifierType())
    registry.register(LootModifierKeys.ENCHANTED_COUNT_INCREASE, EnchantedCountIncreaseLootModifierType())
    registry.register(LootModifierKeys.EXPLORATION_MAP, ExplorationMapLootModifierType())
    registry.register(LootModifierKeys.EXPLOSION_DECAY, ExplosionDecayLootModifierType())
    registry.register(LootModifierKeys.FILTERED, FilteredLootModifierType())
    registry.register(LootModifierKeys.FURNACE_SMELT, FurnaceSmeltLootModifierType())
    registry.register(LootModifierKeys.LIMIT_COUNT, LimitCountLootModifierType())
    registry.register(LootModifierKeys.SET_COMPONENTS, SetComponentsLootModifierType())
    registry.register(LootModifierKeys.SET_DAMAGE, SetDamageLootModifierType())
    registry.register(LootModifierKeys.SET_ENCHANTMENTS, SetEnchantmentsLootModifierType())
    registry.register(LootModifierKeys.SET_INSTRUMENT, SetInstrumentLootModifierType())
    registry.register(LootModifierKeys.SET_NAME, SetNameLootModifierType())
    registry.register(LootModifierKeys.SET_OMINOUS_BOTTLE_AMPLIFIER, SetOminousBottleAmplifierLootModifierType())
    registry.register(LootModifierKeys.SET_STEW_EFFECT, SetStewEffectLootModifierType())
    registry.register(LootModifierKeys.DISCARD, DiscardLootModifierType())
}

@Suppress("UnstableApiUsage")
fun createVanillaLootModifierTypeRegistry(): DynamicRegistry<LootModifierType> =
    DynamicRegistry.create<LootModifierType>(Key.key("twaddle:vanilla_loot_modifier_types")).also(
        ::addAllVanillaLootModifierTypes,
    )
