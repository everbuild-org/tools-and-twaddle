package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderArithmetic
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderCondition
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderDeclaration
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderKeys
import org.everbuild.twaddle.datapack_primitives.number_provider.NumberProviderType
import org.everbuild.twaddle.datapack_primitives.number_provider.context.FloatStorageContext
import org.everbuild.twaddle.datapack_primitives.number_provider.context.IntStorageContext
import org.everbuild.twaddle.datapack_primitives.number_provider.context.NumberProviderContext
import java.util.Random

private data class StorageDeclaration<T>(
    val storage: Key,
    val path: String,
    val fallback: NumberProviderDeclaration<T>?,
    val arithmetic: NumberProviderArithmetic<T>,
) : NumberProviderDeclaration<T> {
    override fun type() = NumberProviderKeys.STORAGE

    override fun get(context: NumberProviderContext, random: Random): T {
        val value = if (arithmetic.valueCodec == Codec.INT) {
            context.getBehaviour(IntStorageContext).getValue(storage, path)
        } else {
            context.getBehaviour(FloatStorageContext).getValue(storage, path)
        }
        @Suppress("UNCHECKED_CAST")
        return (value as? T) ?: fallback?.get(context, random) ?: arithmetic.zero
    }
}

internal class StorageNumberProviderType<T> : NumberProviderType<T> {
    override fun codec(
        providerCodec: Codec<NumberProviderDeclaration<T>>,
        floatProviderCodec: Codec<NumberProviderDeclaration<Float>>?,
        conditionCodec: Codec<NumberProviderCondition>,
        arithmetic: NumberProviderArithmetic<T>,
    ) = StructCodec.struct(
        "storage", Codec.KEY, { (it as StorageDeclaration<T>).storage },
        "path", Codec.STRING, { it.path },
        "fallback", providerCodec.optional(), { it.fallback },
        { storage, path, fallback -> StorageDeclaration(storage, path, fallback, arithmetic) },
    ).asProviderCodec()
}
