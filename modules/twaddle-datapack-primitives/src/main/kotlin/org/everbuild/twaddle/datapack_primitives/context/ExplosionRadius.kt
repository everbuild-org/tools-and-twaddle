package org.everbuild.twaddle.datapack_primitives.context

/** Explosion radius associated with the loot context, or null when no explosion is present. */
val ExplosionRadiusContext = DataContextKey<Float?>("twaddle:explosion_radius_context")

@JvmSynthetic
fun DataContext.withExplosionRadius(radius: Float?) = withBehaviour(ExplosionRadiusContext, radius)
