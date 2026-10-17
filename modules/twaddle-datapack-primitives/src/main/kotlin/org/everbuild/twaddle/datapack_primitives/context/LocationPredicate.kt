package org.everbuild.twaddle.datapack_primitives.context

import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.utils.Either

/** Inclusive numeric bounds used by location predicates. A missing bound is unbounded. */
data class LocationRange<T>(val min: T? = null, val max: T? = null)

/** An identifier or a `#`-prefixed tag accepted by registry-backed predicate fields. */
data class RegistryEntryReference(val key: Key, val tag: Boolean = false)

/** A set of registry identifiers and tags. */
data class RegistryEntrySet(val values: List<RegistryEntryReference>)

/** Coordinate constraints in the nested vanilla location predicate. */
data class LocationPositionPredicate(
    val x: LocationRange<Double>?,
    val y: LocationRange<Double>?,
    val z: LocationRange<Double>?,
)

/**
 * Typed outer structure of a vanilla location predicate.
 *
 * Block and fluid predicates are kept as raw JSON for now because their component/state sub-schemas
 * have their own codecs and registry dependencies.
 */
data class LocationPredicate(
    val position: LocationPositionPredicate? = null,
    val biomes: RegistryEntrySet? = null,
    val structures: RegistryEntrySet? = null,
    val dimension: Key? = null,
    val smokey: Boolean? = null,
    val light: LocationRange<Int>? = null,
    val block: BlockLocationPredicate? = null,
    val fluid: FluidLocationPredicate? = null,
    val canSeeSky: Boolean? = null,
)

/** Codec for vanilla's scalar-or-bounds representation of a numeric range. */
fun <T : Comparable<T>> locationRangeCodec(valueCodec: Codec<T>): Codec<LocationRange<T>> {
    val boundsCodec = StructCodec.struct(
        "min", valueCodec.optional(), LocationRange<T>::min,
        "max", valueCodec.optional(), LocationRange<T>::max,
        ::LocationRange,
    )
    return Codec.Either<T, LocationRange<T>>(valueCodec, boundsCodec).transform(
        { encoded -> encoded.unify({ LocationRange(it, it) }, { it }) },
        { range ->
            if (range.min != null && range.min == range.max) {
                Either.left(range.min)
            } else {
                Either.right(range)
            }
        },
    )
}

internal val registryEntrySetCodec: Codec<RegistryEntrySet> = Codec.Either(
    Codec.STRING,
    Codec.STRING.list(),
).transform(
    { encoded ->
        val values = encoded.unify({ listOf(it) }, { it })
        RegistryEntrySet(values.map(::parseRegistryEntryReference))
    },
    { set ->
        val values = set.values.map { reference ->
            (if (reference.tag) "#" else "") + reference.key.asString()
        }
        if (values.size == 1) Either.left(values.single()) else Either.right(values)
    },
)

private fun parseRegistryEntryReference(value: String): RegistryEntryReference {
    val tag = value.startsWith('#')
    val keyString = if (tag) value.drop(1) else value
    require(keyString.isNotBlank()) { "Registry tag must include an identifier" }
    return RegistryEntryReference(Key.key(keyString), tag)
}

private val positionCodec = StructCodec.struct(
    "x", locationRangeCodec(Codec.DOUBLE).optional(), LocationPositionPredicate::x,
    "y", locationRangeCodec(Codec.DOUBLE).optional(), LocationPositionPredicate::y,
    "z", locationRangeCodec(Codec.DOUBLE).optional(), LocationPositionPredicate::z,
    ::LocationPositionPredicate,
)

/** Codec for the outer vanilla location-predicate object. */
val LOCATION_PREDICATE_CODEC: Codec<LocationPredicate> = StructCodec.struct(
    "position", positionCodec.optional(), LocationPredicate::position,
    "biomes", registryEntrySetCodec.optional(), LocationPredicate::biomes,
    "structures", registryEntrySetCodec.optional(), LocationPredicate::structures,
    "dimension", Codec.KEY.optional(), LocationPredicate::dimension,
    "smokey", Codec.BOOLEAN.optional(), LocationPredicate::smokey,
    "light", locationRangeCodec(Codec.INT).optional(), LocationPredicate::light,
    "block", BLOCK_LOCATION_PREDICATE_CODEC.optional(), LocationPredicate::block,
    "fluid", FLUID_LOCATION_PREDICATE_CODEC.optional(), LocationPredicate::fluid,
    "can_see_sky", Codec.BOOLEAN.optional(), LocationPredicate::canSeeSky,
    ::LocationPredicate,
)
