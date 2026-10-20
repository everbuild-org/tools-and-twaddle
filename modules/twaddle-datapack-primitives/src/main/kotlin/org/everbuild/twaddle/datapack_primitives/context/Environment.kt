package org.everbuild.twaddle.datapack_primitives.context

import net.minestom.server.instance.Instance
import net.minestom.server.world.DimensionType
import net.minestom.server.world.attribute.EnvironmentAttributeMap

val EnvironmentContext = DataContextKey<EnvironmentAttributeMap>("twaddle:environment_context")

@JvmSynthetic
fun DataContext.withEnvironment(map: EnvironmentAttributeMap) = withBehaviour(EnvironmentContext, map)

@JvmSynthetic
fun DataContext.withEnvironment(dimension: DimensionType) =
    withBehaviour(EnvironmentContext, dimension.attributes())

@JvmSynthetic
fun DataContext.withEnvironment(instance: Instance) =
    @Suppress("UnstableApiUsage")
    withEnvironment(instance.cachedDimensionType)
