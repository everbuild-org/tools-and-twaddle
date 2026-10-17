package org.everbuild.twaddle.datapack_primitives.number_provider

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.number_provider.impl.ConstantNumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate

/** Builds a recursive provider codec, including the vanilla scalar form for constants. */
fun <T> createNumberProviderCodec(
    registry: Registry<org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType<T>>,
    conditionCodec: Codec<ContextPredicate>,
    arithmetic: org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic<T>,
    floatProviderCodec: Codec<org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration<Float>>? = null,
): Codec<org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration<T>> = Codec.Recursive { providerCodec ->
    require(arithmetic.binomial == null || floatProviderCodec != null) {
        "The binomial provider requires a float number-provider codec"
    }
    val taggedCodec = Codec.STRING.unionType(
        "type",
        { id -> registry.get(Key.key(id))?.codec(providerCodec, floatProviderCodec, conditionCodec, arithmetic) },
        { declaration ->
            val type = declaration.type()
            registry.getKey(type)?.key()?.asString()
        }
    )

    Codec.Either<T, NumberProviderDeclaration<T>>(arithmetic.valueCodec, taggedCodec).transform(
        { encoded ->
            encoded.unify<NumberProviderDeclaration<T>>(
                { value -> ConstantNumberProviderDeclaration(value) },
                { declaration -> declaration },
            )
        },
        { declaration ->
            if (declaration is ConstantNumberProviderDeclaration<*>) {
                @Suppress("UNCHECKED_CAST")
                net.minestom.server.utils.Either.left<T, NumberProviderDeclaration<T>>(declaration.value as T)
            } else {
                net.minestom.server.utils.Either.right<T, NumberProviderDeclaration<T>>(declaration)
            }
        },
    )
}
