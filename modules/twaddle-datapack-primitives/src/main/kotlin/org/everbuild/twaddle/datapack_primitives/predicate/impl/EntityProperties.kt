package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicateContext
import org.everbuild.twaddle.datapack_primitives.context.EntityPredicate
import org.everbuild.twaddle.datapack_primitives.context.rawEntityPredicateCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class EntityPropertiesPredicate(val entity: String, val predicate: EntityPredicate?) : ContextPredicate {
    override fun type() = ContextPredicateKeys.ENTITY_PROPERTIES
    override fun test(context: DataContext, random: Random): Boolean = predicate?.let {
        context[EntityPredicateContext].matches(entity, it)
    } ?: true
}

internal class EntityPropertiesPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = codec(
        predicateCodec,
        intProviderCodec,
        floatProviderCodec,
        rawEntityPredicateCodec,
    )

    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
        entityPredicateCodec: Codec<EntityPredicate>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "entity", Codec.STRING, EntityPropertiesPredicate::entity,
        "predicate", entityPredicateCodec.optional(), EntityPropertiesPredicate::predicate,
        ::EntityPropertiesPredicate,
    ).asContextPredicateCodec()
}
