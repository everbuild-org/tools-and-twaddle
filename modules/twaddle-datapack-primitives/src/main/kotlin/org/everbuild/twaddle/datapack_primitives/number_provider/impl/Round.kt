package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import kotlin.math.floor

internal class RoundNumberProviderType : FloatUnaryNumberProviderType(Key.key("minecraft:round"), { floor(it + 0.5f) })
