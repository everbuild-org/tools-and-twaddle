package org.everbuild.twaddle.datapack_primitives.context

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec

/** Block criteria nested in a location predicate. Component values stay opaque until their codecs are available. */
data class BlockLocationPredicate(
    val blocks: RegistryEntrySet? = null,
    val state: Map<String, LocationRange<String>>? = null,
    val nbt: Codec.RawValue? = null,
    val components: Map<Key, Codec.RawValue>? = null,
    val predicates: Map<Key, Codec.RawValue>? = null,
)

/** Codec for the advancement-style block predicate used by location predicates. */
val BLOCK_LOCATION_PREDICATE_CODEC: Codec<BlockLocationPredicate> = StructCodec.struct(
    "blocks", registryEntrySetCodec.optional(), BlockLocationPredicate::blocks,
    "state", Codec.STRING.mapValue(locationRangeCodec(Codec.STRING)).optional(), BlockLocationPredicate::state,
    "nbt", Codec.RAW_VALUE.optional(), BlockLocationPredicate::nbt,
    "components", Codec.KEY.mapValue(Codec.RAW_VALUE).optional(), BlockLocationPredicate::components,
    "predicates", Codec.KEY.mapValue(Codec.RAW_VALUE).optional(), BlockLocationPredicate::predicates,
    ::BlockLocationPredicate,
)
