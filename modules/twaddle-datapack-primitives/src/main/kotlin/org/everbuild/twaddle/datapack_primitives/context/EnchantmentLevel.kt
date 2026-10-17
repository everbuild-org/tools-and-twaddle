package org.everbuild.twaddle.datapack_primitives.context

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.component.EnchantmentList

val EnchantmentLevelContext = DataContextKey<EnchantmentList?>("twaddle:enchantment_level")

@JvmSynthetic
fun DataContext.withEnchantment(list: EnchantmentList?) = withBehaviour(EnchantmentLevelContext, list)

@JvmSynthetic
fun DataContext.withEnchantment(stack: ItemStack) = withBehaviour(
    EnchantmentLevelContext,
    stack.get(DataComponents.ENCHANTMENTS)
)
