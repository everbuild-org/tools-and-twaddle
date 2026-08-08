package org.everbuild.twaddle.common.block

import net.minestom.server.instance.block.Block

interface MaterializedBlockSelector : BlockSelector {
    val blocks: List<Block>

    override fun matches(block: Block): Boolean = blocks.contains(block)
}
