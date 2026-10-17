package org.everbuild.twaddle.datapack_primitives.context

import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec

/** Fluid criteria nested in a location predicate. */
data class FluidLocationPredicate(
    val fluids: RegistryEntrySet? = null,
    val state: Map<String, LocationRange<String>>? = null,
)

/** Codec for the current vanilla fluid-predicate shape. */
val FLUID_LOCATION_PREDICATE_CODEC: Codec<FluidLocationPredicate> = StructCodec.struct(
    "fluids", registryEntrySetCodec.optional(), FluidLocationPredicate::fluids,
    "state", Codec.STRING.mapValue(locationRangeCodec(Codec.STRING)).optional(), FluidLocationPredicate::state,
    ::FluidLocationPredicate,
)
