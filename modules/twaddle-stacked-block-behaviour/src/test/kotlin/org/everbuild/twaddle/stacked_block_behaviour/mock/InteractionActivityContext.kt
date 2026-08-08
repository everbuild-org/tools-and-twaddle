package org.everbuild.twaddle.stacked_block_behaviour.mock

import net.minestom.server.coordinate.Vec
import net.minestom.server.entity.Player
import net.minestom.server.entity.PlayerHand
import net.minestom.server.instance.Instance
import net.minestom.server.instance.block.Block
import net.minestom.server.instance.block.BlockFace
import org.everbuild.twaddle.stacked_block_behaviour.type.InteractionActivityContext

fun InteractionActivityContext.Companion.mock(instance: Instance, player: Player): InteractionActivityContext =
    InteractionActivityContext(
        block = Block.STONE,
        instance = instance,
        blockFace = BlockFace.TOP,
        blockPosition = Vec.ZERO,
        cursorPosition = Vec.ZERO,
        player = player,
        hand = PlayerHand.MAIN,
    )