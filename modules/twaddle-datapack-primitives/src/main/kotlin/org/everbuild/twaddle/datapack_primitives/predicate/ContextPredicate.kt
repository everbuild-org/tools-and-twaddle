package org.everbuild.twaddle.datapack_primitives.predicate

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import java.util.Random

interface ContextPredicate {
    fun type(): Key

    fun test(context: DataContext, random: Random): Boolean
}
