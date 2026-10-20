package org.everbuild.twaddle.datapack_primitives.slot_source

import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.core.testSuite
import de.infix.testBalloon.framework.shared.TestRegistering
import org.everbuild.twaddle.data_driven.pattern.PackFolderPattern
import org.everbuild.twaddle.data_driven.resource.ClasspathSource
import org.everbuild.twaddle.data_driven.testing.codecFixtures
import org.everbuild.twaddle.datapack_primitives.DatapackPrimitives

object SlotSourceSmokeTestAnchor

@TestRegistering
private fun TestSuiteScope.slotSourceFixtures(primitives: DatapackPrimitives) {
    codecFixtures(
        name = "Slot sources",
        source = ClasspathSource(SlotSourceSmokeTestAnchor::class),
        pattern = PackFolderPattern().resource("slot_source", ".json"),
        codec = primitives.slotSourceCodec,
    )
}

val SlotSourceSmokeTests by testSuite {
    slotSourceFixtures(DatapackPrimitives.vanilla())
}
