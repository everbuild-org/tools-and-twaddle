package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.DamageSourcePredicateContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class DamageSourcePropertiesPredicate(val predicate: Codec.RawValue) : ContextPredicate {
    override fun type() = ContextPredicateKeys.DAMAGE_SOURCE_PROPERTIES
    override fun test(context: DataContext, random: Random) =
        context[DamageSourcePredicateContext].matches(predicate)
}

internal class DamageSourcePropertiesPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "predicate", Codec.RAW_VALUE, DamageSourcePropertiesPredicate::predicate,
        ::DamageSourcePropertiesPredicate,
    ).asContextPredicateCodec()
}
