package org.everbuild.twaddle.data_driven.pattern

import net.kyori.adventure.key.Key

/** Matches normalized source-relative paths and derives the registry key for a match. */
interface ResourcePattern {
    fun key(path: String): Key?
}