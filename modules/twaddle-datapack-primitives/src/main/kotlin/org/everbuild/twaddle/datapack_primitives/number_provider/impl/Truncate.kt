package org.everbuild.twaddle.datapack_primitives.number_provider.impl

import net.kyori.adventure.key.Key
import kotlin.math.truncate

internal class TruncateNumberProviderType : FloatUnaryNumberProviderType(Key.key("minecraft:truncate"), ::truncate)
