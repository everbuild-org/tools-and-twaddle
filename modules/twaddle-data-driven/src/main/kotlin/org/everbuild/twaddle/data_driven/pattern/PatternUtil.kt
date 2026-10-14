package org.everbuild.twaddle.data_driven.pattern

import net.kyori.adventure.key.Key

internal fun parseKeyOrNull(value: String): Key? = try {
    Key.key(value)
} catch (_: RuntimeException) {
    null
}
