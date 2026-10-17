package org.everbuild.twaddle.datapack_primitives.number_provider

import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import org.everbuild.twaddle.data_driven.pattern.PackFolderPattern
import org.everbuild.twaddle.data_driven.resource.ClasspathSource
import org.everbuild.twaddle.data_driven.testing.codecFixtures
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives

object SmokeTestAnchor

@TestRegistering
private fun TestSuiteScope.numberProviderTests(primitives: DatapackPrimitives) {
    codecFixtures(
        name = "Integer number providers",
        source = ClasspathSource(SmokeTestAnchor::class),
        pattern = PackFolderPattern().resource("worldgen/context_int_provider", ".json"),
        codec = primitives.intNumberProviderCodec,
    )
    codecFixtures(
        name = "Float number providers",
        source = ClasspathSource(SmokeTestAnchor::class),
        pattern = PackFolderPattern().resource("worldgen/context_float_provider", ".json"),
        codec = primitives.floatNumberProviderCodec,
    )
    codecFixtures(
        name = "Context predicates",
        source = ClasspathSource(SmokeTestAnchor::class),
        pattern = PackFolderPattern().resource("predicate", ".json"),
        codec = primitives.contextPredicateCodec,
    )
}

val DatapackPrimitiveSmokeTests by testSuite {
    numberProviderTests(DatapackPrimitives.vanilla())
}
