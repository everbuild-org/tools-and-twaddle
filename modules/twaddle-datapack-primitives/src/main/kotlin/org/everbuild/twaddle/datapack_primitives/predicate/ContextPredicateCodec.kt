package org.everbuild.twaddle.datapack_primitives.predicate

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicate
import org.everbuild.twaddle.datapack_primitives.context.rawEntityPredicateCodec
import net.minestom.server.utils.Either

/** Builds a recursive codec for registered context predicates. */
fun createContextPredicateCodec(
    registry: Registry<ContextPredicateType>,
    intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
    floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
): Codec<ContextPredicate> = createContextPredicateCodec(
    registry,
    intProviderCodec,
    floatProviderCodec,
    rawEntityPredicateCodec,
)

/** Builds a recursive codec for registered context predicates and nested entity predicates. */
fun createContextPredicateCodec(
    registry: Registry<ContextPredicateType>,
    intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
    floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    entityPredicateCodec: Codec<EntityPredicate>,
): Codec<ContextPredicate> = Codec.Recursive { predicateCodec ->
    val typedPredicateCodec = Codec.STRING.unionType(
        "type",
        {
                id -> registry.get(Key.key(id))?.codec(
                    predicateCodec,
                    intProviderCodec,
                    floatProviderCodec,
                    entityPredicateCodec,
                )
        },
        { predicate -> registry.getKey(predicate.type())?.key()?.asString() },
    )
    Codec.Either(Codec.KEY, typedPredicateCodec).transform(
        { encoded -> encoded.unify(::ContextPredicateReference, { it }) },
    ) { predicate ->
        if (predicate is ContextPredicateReference) Either.left(predicate.key)
        else Either.right(predicate)
    }
}
