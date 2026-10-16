package org.everbuild.twaddle.datapack_primitives.number_provider

import org.everbuild.twaddle.datapack_primitives.number_provider.context.NumberProviderContext
import java.util.Random

fun interface NumberProviderCondition {
    fun test(context: NumberProviderContext, random: Random): Boolean
}
