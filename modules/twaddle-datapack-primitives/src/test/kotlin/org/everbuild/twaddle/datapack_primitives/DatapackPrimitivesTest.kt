package org.everbuild.twaddle.datapack_primitives

import de.infix.testBalloon.framework.core.testSuite
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldNotBe
import net.kyori.adventure.key.Key

val datapackPrimitivesTests by testSuite("Datapack primitives registry bundle") {
    test("Vanilla registries and codecs are assembled together") {
        val primitives = DatapackPrimitives.vanilla()

        primitives.intNumberProviderTypes.get(Key.key("minecraft:constant")) shouldNotBe null
        primitives.floatNumberProviderTypes.get(Key.key("minecraft:constant")) shouldNotBe null
        primitives.contextPredicateTypes.get(Key.key("minecraft:all_of")) shouldNotBe null
        primitives.intNumberProviderCodec shouldNotBe null
        primitives.floatNumberProviderCodec shouldNotBe null
        primitives.contextPredicateCodec shouldNotBe null
    }

    test("Builder cannot be reused after build") {
        val builder = DatapackPrimitives.builder()
        builder.build()

        shouldThrow<IllegalStateException> { builder.build() }
    }
}
