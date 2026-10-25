package org.everbuild.twaddle.loot.modifier.context

import net.minestom.server.component.DataComponentMap
import net.minestom.server.registry.Registries
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DataContextKey

/** Resolves the Minestom registries used by registry-backed loot modifiers. */
val LootModifierRegistriesContext = DataContextKey<Registries>("twaddle:loot_modifier_registries")

/** Provides component data from the source named by a `copy_components` modifier. */
@Suppress("UnstableApiUsage")
fun interface LootComponentSourceAccess {
    fun getComponents(source: String): DataComponentMap?
}

val LootComponentSourceAccessContext = DataContextKey<LootComponentSourceAccess>("twaddle:loot_component_source_access")
val LootAdditionalTradeCostAllowedContext = DataContextKey<Boolean>("twaddle:loot_additional_trade_cost_allowed")

@JvmSynthetic
fun DataContext.withLootModifierRegistries(registries: Registries) =
    withBehaviour(LootModifierRegistriesContext, registries)

internal fun DataContext.enchantmentRegistry() =
    getBehaviour(LootModifierRegistriesContext).enchantment()

@JvmSynthetic
fun DataContext.withLootComponentSourceAccess(access: LootComponentSourceAccess) =
    withBehaviour(LootComponentSourceAccessContext, access)

@JvmSynthetic
fun DataContext.withLootAdditionalTradeCostAllowed(allowed: Boolean) =
    withBehaviour(LootAdditionalTradeCostAllowedContext, allowed)

internal fun DataContext.lootAdditionalTradeCostAllowed(): Boolean =
    getBehaviourOrNull(LootAdditionalTradeCostAllowedContext) ?: false
