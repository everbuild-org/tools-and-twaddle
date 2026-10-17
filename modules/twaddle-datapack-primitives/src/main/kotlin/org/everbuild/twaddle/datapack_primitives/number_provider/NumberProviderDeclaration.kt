package org.everbuild.twaddle.datapack_primitives.number_provider

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

interface NumberProviderDeclaration<T> {
    fun type(): Key
    fun get(context: DataContext, random: Random): T
}