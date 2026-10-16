package org.everbuild.twaddle.datapack_primitives.number_provider

import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import net.kyori.adventure.key.Key
import net.minestom.server.codec.Codec
import net.minestom.server.codec.StructCodec
import net.minestom.server.registry.DynamicRegistry
import org.everbuild.twaddle.data_driven.pattern.PackFolderPattern
import org.everbuild.twaddle.data_driven.resource.ClasspathSource
import org.everbuild.twaddle.data_driven.testing.codecFixtures
import org.everbuild.twaddle.datapack_primitives.number_provider.context.NumberProviderContext
import java.util.*

object SmokeTestAnchor

class AlwaysTrueCondition : NumberProviderCondition {
    override fun test(
        context: NumberProviderContext,
        random: Random
    ): Boolean = true
}

@TestRegistering
private fun <T> TestSuiteScope.numberProviderTests(
    arithmetic: NumberProviderArithmetic<T>,
    path: String,
    floatCodec: Codec<NumberProviderDeclaration<Float>>? = null
): Codec<NumberProviderDeclaration<T>> {
    @Suppress("UnstableApiUsage")
    val typeRegistry = DynamicRegistry.create<NumberProviderType<T>>(Key.key("twaddle:types"))
    addAllVanillaNumberProviders(typeRegistry, arithmetic)
    val codec = createNumberProviderCodec(
        typeRegistry,
        StructCodec.struct(AlwaysTrueCondition()),
        arithmetic,
        floatCodec
    )
    codecFixtures(
        name = "NumberProvider @ $path",
        source = ClasspathSource(SmokeTestAnchor::class),
        pattern = PackFolderPattern().resource(path, ".json"),
        codec = codec,
    )

    return codec
}

val NumberProviderSmokeTests by testSuite {
    val floatCodec = numberProviderTests(FLOAT_NUMBER_PROVIDER_ARITHMETIC, "worldgen/context_float_provider")
    numberProviderTests(INT_NUMBER_PROVIDER_ARITHMETIC, "worldgen/context_int_provider", floatCodec)
}
