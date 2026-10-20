package org.everbuild.twaddle.loot.modifier

import net.kyori.adventure.key.Key

/** Opaque caller-defined representation of a loot modifier. */
interface LootModifier {
    fun type(): Key
}
