package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import kotlin.math.sin

internal class SinNumberProviderType : FloatUnaryNumberProviderType(Key.key("minecraft:sin"), ::sin)
