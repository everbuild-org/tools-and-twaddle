@file:Suppress("UnstableApiUsage")

package org.everbuild.twaddle.common.block.util

import net.minestom.server.instance.Chunk

inline fun <T> Chunk.withReadLock(block: () -> T): T {
    lockReadLock()
    return try {
        block()
    } finally {
        unlockReadLock()
    }
}

inline fun <T> Chunk.withWriteLock(block: () -> T): T {
    lockWriteLock()
    return try {
        block()
    } finally {
        unlockWriteLock()
    }
}