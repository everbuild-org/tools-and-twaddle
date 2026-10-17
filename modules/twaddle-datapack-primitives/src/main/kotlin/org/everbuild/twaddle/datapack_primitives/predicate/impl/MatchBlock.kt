package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.context.DataContext
import org.everbuild.twaddle.datapack_primitives.context.BlockPredicateContext
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateKeys
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicateType
import java.util.Random

private data class MatchBlockPredicate(
    val blocks: Codec.RawValue?,
    val state: Codec.RawValue?,
    val nbt: Codec.RawValue?,
    val components: Codec.RawValue?,
    val predicates: Codec.RawValue?,
) : ContextPredicate {
    override fun type() = ContextPredicateKeys.MATCH_BLOCK
    override fun test(context: DataContext, random: Random): Boolean = context[BlockPredicateContext].matches(
        buildMap {
            blocks?.let { put("blocks", it) }
            state?.let { put("state", it) }
            nbt?.let { put("nbt", it) }
            components?.let { put("components", it) }
            predicates?.let { put("predicates", it) }
        },
    )
}

internal class MatchBlockPredicateType : ContextPredicateType {
    override fun codec(
        predicateCodec: Codec<ContextPredicate>,
        intProviderCodec: Codec<NumberProviderDeclaration<Int>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>,
    ): StructCodec<out ContextPredicate> = StructCodec.struct(
        "blocks", Codec.RAW_VALUE.optional(), MatchBlockPredicate::blocks,
        "state", Codec.RAW_VALUE.optional(), MatchBlockPredicate::state,
        "nbt", Codec.RAW_VALUE.optional(), MatchBlockPredicate::nbt,
        "components", Codec.RAW_VALUE.optional(), MatchBlockPredicate::components,
        "predicates", Codec.RAW_VALUE.optional(), MatchBlockPredicate::predicates,
        ::MatchBlockPredicate,
    ).asContextPredicateCodec()
}
