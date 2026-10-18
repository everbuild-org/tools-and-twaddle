package org.everbuild.twaddle.loot.modifier

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.registry.Registry
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

/** Builds a recursive codec for modifiers whose types are registered by their caller. */
fun createLootModifierCodec(
    registry: Registry<LootModifierType>,
    predicateCodec: Codec<ContextPredicate>,
    intNumberProviderCodec: Codec<NumberProviderDeclaration<Int>>,
    floatNumberProviderCodec: Codec<NumberProviderDeclaration<Float>>,
): Codec<LootModifier> = Codec.Recursive { modifierCodec ->
    Codec.STRING.unionType(
        "type",
        { id ->
            registry.get(Key.key(id))?.codec(
                modifierCodec,
                predicateCodec,
                intNumberProviderCodec,
                floatNumberProviderCodec,
            )
        },
        { modifier -> registry.getKey(modifier.type())?.key()?.asString() },
    )
}
