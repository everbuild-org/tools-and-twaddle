package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import kotlin.math.ceil

internal class CeilNumberProviderType : FloatUnaryNumberProviderType(Key.key("minecraft:ceil"), { ceil(it) })
