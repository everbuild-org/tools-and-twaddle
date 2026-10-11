package org.everbuild.twaddle.common.block.tick

import net.minestom.server.coordinate.BlockVec
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block

interface RandomTickHandler {

    /**
     * Handles a random tick for a block.
     *
     * @return the replacement block to apply at the ticked position,
     *         or null to leave the block unchanged.
     */
    fun onRandomTick(randomTick: RandomTick): Block?

    /**
     * Determines whether a block should receive a random tick. This may be a more expensive way to stop ticking than
     * not implementing [RandomTickHandler] but less expensive than just doing nothing in [onRandomTick].
     *
     * As with the whole interface, this does not affect random tick sampling; it only determines whether
     * [onRandomTick] is invoked for an already selected block.
     *
     * @return true if the block should receive a random tick, false otherwise.
     */
    fun shouldRandomTick(block: Block): Boolean = true

    data class RandomTick(
        val block: Block,
        val instance: Instance,
        val blockPosition: BlockVec
    )
}