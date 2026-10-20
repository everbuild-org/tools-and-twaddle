package org.everbuild.twaddle.datapack_primitives.predicate.impl

import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.predicate.ContextPredicate

@Suppress("UNCHECKED_CAST")
internal fun <T : ContextPredicate> StructCodec<T>.asContextPredicateCodec(): StructCodec<ContextPredicate> =
    this as StructCodec<ContextPredicate>
