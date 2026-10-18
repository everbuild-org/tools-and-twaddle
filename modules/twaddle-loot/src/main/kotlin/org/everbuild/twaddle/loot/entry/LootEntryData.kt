package org.everbuild.twaddle.loot.entry

import net.kyori.adventure.key.Key

/** Opaque caller-defined representation of a loot entry. */
interface LootEntryData {
    fun type(): Key
}
