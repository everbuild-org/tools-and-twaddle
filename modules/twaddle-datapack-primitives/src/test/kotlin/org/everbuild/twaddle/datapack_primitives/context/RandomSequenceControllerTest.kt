package org.everbuild.twaddle.datapack_primitives.context

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import net.kyori.adventure.key.Key

val RandomSequenceControllerTests by testSuite("Random sequence controller") {
    test("the default controller reuses a stream for each sequence") {
        val controller = DefaultRandomSequenceController()
        val sequence = Key.key("test:sequence")

        val first = controller.getRandom(sequence)
        first.nextLong()

        controller.getRandom(sequence) shouldBe first
    }

    test("the default controller creates separate streams for different sequences") {
        val controller = DefaultRandomSequenceController()

        controller.getRandom(Key.key("test:first")) shouldNotBe
            controller.getRandom(Key.key("test:second"))
    }

    test("a controller can be supplied through the data context") {
        val controller = DefaultRandomSequenceController()
        val context = DataContext().withRandomSequenceController(controller)

        context[RandomSequenceControllerContext] shouldBe controller
    }
}
