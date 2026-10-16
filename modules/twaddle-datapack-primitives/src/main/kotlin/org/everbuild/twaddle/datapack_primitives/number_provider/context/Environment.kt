package org.everbuild.twaddle.datapack_primitives.number_provider.context

import net.minestom.server.instance.Instance
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.EnvironmentAttributeMap

val EnvironmentContext = NumberProviderContextKey<EnvironmentAttributeMap>("twaddle:environment_context")

@JvmSynthetic
fun NumberProviderContext.withEnvironment(map: EnvironmentAttributeMap) = withBehaviour(EnvironmentContext, map)

@JvmSynthetic
fun NumberProviderContext.withEnvironment(dimension: DimensionType) =
    withBehaviour(EnvironmentContext, dimension.attributes())

@JvmSynthetic
fun NumberProviderContext.withEnvironment(instance: Instance) =
    @Suppress("UnstableApiUsage")
    withEnvironment(instance.cachedDimensionType)
