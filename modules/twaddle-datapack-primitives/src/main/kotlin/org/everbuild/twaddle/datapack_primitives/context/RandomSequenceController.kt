package org.everbuild.twaddle.datapack_primitives.context

import net.kyori.adventure.key.Key
import java.util.Random
import java.util.concurrent.ConcurrentHashMap

/** Supplies stateful random streams for Minecraft's named random sequences. */
fun interface RandomSequenceController {
    /**
     * Returns the stream for [sequence]. Calls for the same key must continue the same stream,
     * while different keys must have independent streams.
     *
     * Implementations are responsible for creating streams with the desired world seed and
     * Minecraft random-sequence settings.
     */
    fun getRandom(sequence: Key): Random
}

/** Creates an unseeded [Random] automatically the first time each sequence is requested. */
class DefaultRandomSequenceController : RandomSequenceController {
    private val sequences = ConcurrentHashMap<Key, Random>()

    override fun getRandom(sequence: Key): Random =
        sequences.computeIfAbsent(sequence) { Random() }
}

val RandomSequenceControllerContext =
    DataContextKey<RandomSequenceController>("twaddle:random_sequence_controller")

@JvmSynthetic
fun DataContext.withRandomSequenceController(controller: RandomSequenceController) =
    withBehaviour(RandomSequenceControllerContext, controller)
