package org.everbuild.twaddle.datapack_primitives.number_provider.context

import net.kyori.adventure.key.Key
import net.minestom.server.tag.Tag
import net.minestom.server.tag.TagReadable

interface StorageAccessor<T> {
    fun getValue(value: String): T?

    fun getValue(storage: Key, path: String): T? = getValue(path)
}

class NbtStorageAccessor<T>(val reader: TagReadable, val tagConstructor: (String) -> Tag<T>) : StorageAccessor<T> {
    override fun getValue(value: String): T? {
        val tag = tagConstructor(value)
        return if (reader.hasTag(tag)) reader.getTag(tag) else null
    }
}

val IntStorageContext = NumberProviderContextKey<StorageAccessor<Int>>("twaddle:int_storage_context")
val FloatStorageContext = NumberProviderContextKey<StorageAccessor<Float>>("twaddle:float_storage_context")


@JvmSynthetic
@JvmName("withIntStorage")
fun NumberProviderContext.withStorage(accessor: StorageAccessor<Int>) = withBehaviour(IntStorageContext, accessor)

@JvmSynthetic
@JvmName("withFloatStorage")
fun NumberProviderContext.withStorage(accessor: StorageAccessor<Float>) = withBehaviour(FloatStorageContext, accessor)

@JvmSynthetic
fun NumberProviderContext.withIntNbtStorage(reader: TagReadable) = withBehaviour(
    IntStorageContext,
    NbtStorageAccessor(
        reader
    ) { Tag.Integer(it) }
)

@JvmSynthetic
fun NumberProviderContext.withFloatNbtStorage(reader: TagReadable) = withBehaviour(
    FloatStorageContext,
    NbtStorageAccessor(
        reader
    ) { Tag.Float(it) }
)
