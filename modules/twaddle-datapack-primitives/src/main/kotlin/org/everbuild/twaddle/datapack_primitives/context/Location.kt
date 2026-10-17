package org.everbuild.twaddle.datapack_primitives.context

/** A location supplied by the caller. */
data class ContextLocation(val x: Double, val y: Double, val z: Double)

/** Evaluates a decoded vanilla location predicate at a position. */
fun interface LocationPredicateEvaluator {
    fun matches(position: ContextLocation, predicate: LocationPredicate): Boolean
}

data class LocationContext(
    val origin: ContextLocation,
    val evaluator: LocationPredicateEvaluator = LocationPredicateEvaluator { _, _ -> false },
)

val LocationContextKey = DataContextKey<LocationContext>("twaddle:location_context")

@JvmSynthetic
fun DataContext.withLocation(location: LocationContext) = withBehaviour(LocationContextKey, location)
