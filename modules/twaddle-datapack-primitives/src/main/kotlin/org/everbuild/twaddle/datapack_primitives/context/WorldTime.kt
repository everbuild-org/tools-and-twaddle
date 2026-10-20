package org.everbuild.twaddle.datapack_primitives.context

import net.kyori.adventure.key.Key

/** World clock readings indexed by clock ID, in ticks. */
fun interface WorldTimeAccessor {
    fun time(clock: Key): Long
}

val WorldTimeContext = DataContextKey<WorldTimeAccessor>("twaddle:world_time_context")

@JvmSynthetic
fun DataContext.withWorldTime(accessor: WorldTimeAccessor) = withBehaviour(WorldTimeContext, accessor)
