package org.everbuild.twaddle.data_driven

import net.kyori.adventure.key.Key

/** One immutable, successfully published generation of a [DataDriven] registry. */
@ConsistentCopyVisibility
data class DataSnapshot<T : Any> internal constructor(
    val entries: Map<Key, T>,
    val revision: Long,
)
