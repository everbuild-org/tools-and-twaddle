package org.everbuild.twaddle.datapack_primitives.context

/** Active state of the current enchantment evaluation, or null when the state is unavailable. */
val EnchantmentActiveContext = DataContextKey<Boolean?>("twaddle:enchantment_active_context")

@JvmSynthetic
fun DataContext.withEnchantmentActive(active: Boolean?) = withBehaviour(EnchantmentActiveContext, active)
