import de.infix.testBalloon.framework.core.testSuite
import io.kotest.matchers.shouldBe

val Tests by testSuite {
    test("Happy Path") {
        "abc" shouldBe "abc"
    }
}