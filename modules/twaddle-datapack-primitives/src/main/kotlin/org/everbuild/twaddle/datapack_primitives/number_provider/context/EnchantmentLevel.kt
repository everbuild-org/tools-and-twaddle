package org.everbuild.twaddle.datapack_primitives.number_provider.context

import net.minestom.server.component.DataComponents
import net.minestom.server.item.ItemStack
import net.minestom.server.item.component.EnchantmentList

val EnchantmentLevelContext = NumberProviderContextKey<EnchantmentList?>("twaddle:enchantment_level")

@JvmSynthetic
fun NumberProviderContext.withEnchantment(list: EnchantmentList?) = withBehaviour(EnchantmentLevelContext, list)

@JvmSynthetic
fun NumberProviderContext.withEnchantment(stack: ItemStack) = withBehaviour(
    EnchantmentLevelContext,
    stack.get(DataComponents.ENCHANTMENTS)
)
