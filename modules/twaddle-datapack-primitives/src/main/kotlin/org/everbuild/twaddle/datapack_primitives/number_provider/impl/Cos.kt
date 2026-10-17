package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import kotlin.math.cos

internal class CosNumberProviderType : FloatUnaryNumberProviderType(Key.key("minecraft:cos"), ::cos)
