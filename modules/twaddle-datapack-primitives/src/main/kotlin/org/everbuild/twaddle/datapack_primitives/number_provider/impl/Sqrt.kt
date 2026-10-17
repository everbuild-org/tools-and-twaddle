package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import kotlin.math.sqrt

internal class SqrtNumberProviderType : FloatUnaryNumberProviderType(Key.key("minecraft:sqrt"), ::sqrt)
