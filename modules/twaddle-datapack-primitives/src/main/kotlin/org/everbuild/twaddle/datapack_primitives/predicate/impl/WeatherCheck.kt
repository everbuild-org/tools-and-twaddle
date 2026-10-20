package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.WeatherContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class WeatherCheckPredicate(val raining: Boolean?, val thundering: Boolean?) : ContextPredicate {
    override fun type() = ContextPredicateKeys.WEATHER_CHECK

    override fun test(context: DataContext, random: Random): Boolean {
        val weather = context.getBehaviour(WeatherContext)
        return (raining == null || (weather.raining || weather.thundering) == raining) &&
            (thundering == null || weather.thundering == thundering)
    }
}

internal class WeatherCheckPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "raining", Codec.BOOLEAN.optional(), WeatherCheckPredicate::raining,
        "thundering", Codec.BOOLEAN.optional(), WeatherCheckPredicate::thundering,
        ::WeatherCheckPredicate,
    ).asContextPredicateCodec()
}
