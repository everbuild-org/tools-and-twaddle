package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.utils.Either
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import java.util.Random

internal sealed interface ProviderValueTest<T> {
    data class Exact<T>(val value: NumberProviderDeclaration<T>) : ProviderValueTest<T>
    data class Bounds<T>(
        val min: NumberProviderDeclaration<T>?,
        val max: NumberProviderDeclaration<T>?,
    ) : ProviderValueTest<T>
}

internal fun <T> providerValueTestCodec(
    providerCodec: Codec<NumberProviderDeclaration<T>>,
): Codec<ProviderValueTest<T>> {
    val boundsCodec = StructCodec.struct(
        "min", providerCodec.optional(), { it.min },
        "max", providerCodec.optional(), { it.max },
        ProviderValueTest<T>::Bounds,
    )
    return Codec.Either<NumberProviderDeclaration<T>, ProviderValueTest.Bounds<T>>(
        providerCodec,
        boundsCodec,
    ).transform(
        { value -> value.unify<ProviderValueTest<T>>(ProviderValueTest<T>::Exact, { it }) },
        { test ->
            when (test) {
                is ProviderValueTest.Exact -> Either.left(test.value)
                is ProviderValueTest.Bounds -> Either.right(test)
            }
        },
    )
}

internal fun <T> ProviderValueTest<T>.matches(
    value: T,
    context: DataContext,
    random: Random,
    inRange: (value: T, min: T?, max: T?) -> Boolean,
): Boolean = when (this) {
    is ProviderValueTest.Exact -> value == this.value.get(context, random)
    is ProviderValueTest.Bounds -> inRange(
        value,
        min?.get(context, random),
        max?.get(context, random),
    )
}
