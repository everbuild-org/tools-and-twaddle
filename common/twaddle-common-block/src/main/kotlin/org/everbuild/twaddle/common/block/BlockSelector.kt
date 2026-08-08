package org.everbuild.twaddle.common.block

import net.minestom.server.instance.block.Block

interface BlockSelector {
    fun matches(block: Block): Boolean
}