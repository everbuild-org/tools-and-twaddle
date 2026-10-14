package org.everbuild.twaddle.data_driven.loading

import net.kyori.adventure.key.Key

internal data class Definition<T>(val key: Key, val value: T, val origin: String)