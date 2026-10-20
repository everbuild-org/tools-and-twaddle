package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import kotlin.math.floor

internal class FloorNumberProviderType : FloatUnaryNumberProviderType(Key.key("minecraft:floor"), { floor(it) })
