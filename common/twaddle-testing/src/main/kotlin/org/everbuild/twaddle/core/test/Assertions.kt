package org.everbuild.twaddle.core.test

import io.kotest.assertions.withClue
import io.kotest.matchers.ints.shouldBeLessThanOrEqual

fun shouldBeCalledAtMost(n: Int): () -> Unit {
    var count = 0
    return {
        count++
        withClue("callback should be at most called $n times") {
            count shouldBeLessThanOrEqual n
        }
    }
}