package org.everbuild.twaddle.datapack_primitives.number_provider

import net.minestom.server.codec.Codec
import java.util.Random

/** Supplies scalar encoding and numeric operations for a provider value type such as [Int] or [Float]. */
data class NumberProviderArithmetic<T>(
    val valueCodec: Codec<T>,
    val zero: T,
    val fromInt: (Int) -> T,
    val add: (T, T) -> T,
    val abs: (T) -> T,
    val divide: (T, T) -> T,
    val binomial: ((T, Float, Random) -> T)? = null,
    val multiply: (T, T) -> T,
    val negate: (T) -> T,
    val min: (T, T) -> T,
    val max: (T, T) -> T,
    val mod: (T, T) -> T,
    val floorDivide: (T, T) -> T,
    val floorMod: (T, T) -> T,
    val pow: (T, T) -> T,
    val fromFloat: (Float) -> T,
)
