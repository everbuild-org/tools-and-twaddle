package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.ContextLocation
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.LOCATION_PREDICATE_CODEC
import org.everbuild.twaddle.datapack_primitives.context.LocationContextKey
import org.everbuild.twaddle.datapack_primitives.context.LocationPredicate
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class LocationCheckPredicate(
    val predicate: LocationPredicate?,
    val offsetX: Int,
    val offsetY: Int,
    val offsetZ: Int,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.LOCATION_CHECK

    override fun test(context: DataContext, random: Random): Boolean {
        val location = context.getBehaviour(LocationContextKey)
        val position = ContextLocation(
            location.origin.x + offsetX,
            location.origin.y + offsetY,
            location.origin.z + offsetZ,
        )
        return predicate?.let { location.evaluator.matches(position, it) } ?: true
    }
}

internal class LocationCheckPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "predicate", LOCATION_PREDICATE_CODEC.optional(), LocationCheckPredicate::predicate,
        "offsetX", Codec.INT.optional(0), LocationCheckPredicate::offsetX,
        "offsetY", Codec.INT.optional(0), LocationCheckPredicate::offsetY,
        "offsetZ", Codec.INT.optional(0), LocationCheckPredicate::offsetZ,
        ::LocationCheckPredicate,
    ).asContextPredicateCodec()
}
