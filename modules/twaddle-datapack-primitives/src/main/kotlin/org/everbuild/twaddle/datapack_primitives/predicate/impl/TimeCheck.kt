package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.WorldTimeContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class TimeCheckPredicate(
    val clock: Key,
    val value: ProviderValueTest<Int>,
    val period: Long?,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.TIME_CHECK

    override fun test(context: DataContext, random: Random): Boolean {
        val time = context.getBehaviour(WorldTimeContext).time(clock)
        val checkedTime = period?.let {
            require(it > 0) { "Time-check period must be positive" }
            Math.floorMod(time, it)
        } ?: time
        return when (value) {
            is ProviderValueTest.Exact -> checkedTime == value.value.get(context, random).toLong()
            is ProviderValueTest.Bounds -> {
                val min = value.min?.get(context, random)?.toLong()
                val max = value.max?.get(context, random)?.toLong()
                (min == null || checkedTime >= min) && (max == null || checkedTime <= max)
            }
        }
    }
}

internal class TimeCheckPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> {
        val valueCodec = providerValueTestCodec(intProviderCodec)
        return StructCodec.struct(
            "clock", Codec.KEY, TimeCheckPredicate::clock,
            "value", valueCodec, TimeCheckPredicate::value,
            "period", Codec.LONG.optional(), TimeCheckPredicate::period,
            ::TimeCheckPredicate,
        ).asContextPredicateCodec()
    }
}
