package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration

@Suppress("UNCHECKED_CAST")
internal fun <T, R : NumberProviderDeclaration<T>> StructCodec<R>.asProviderCodec(): StructCodec<NumberProviderDeclaration<T>> =
    this as StructCodec<NumberProviderDeclaration<T>>
