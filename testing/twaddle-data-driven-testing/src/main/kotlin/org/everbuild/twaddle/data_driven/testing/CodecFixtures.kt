package org.everbuild.twaddle.data_driven.testing

import de.infix.testBalloon.framework.core.TestSuiteScope
import de.infix.testBalloon.framework.shared.TestRegistering
import net.minestom.server.codec.Codec
import org.everbuild.twaddle.data_driven.loader.CodecLoader
import org.everbuild.twaddle.data_driven.pattern.ResourcePattern
import org.everbuild.twaddle.data_driven.resource.DataSource

/** Registers one TestBalloon case for each source resource matched by [pattern]. */
@TestRegistering
fun <T : Any> TestSuiteScope.codecFixtures(
    name: String,
    source: DataSource,
    pattern: ResourcePattern,
    codec: Codec<T>,
    verify: (T) -> Unit = {},
) {
    val fixtures = source.discover().filter { pattern.key(it.path) != null }
    require(fixtures.isNotEmpty()) { "No codec fixtures matched '$name' in ${source.description}" }

    val loader = CodecLoader(codec)
    testSuite(name) {
        fixtures.forEach { resource ->
            val key = requireNotNull(pattern.key(resource.path))
            test(key.asString()) {
                verify(loader.load(resource))
            }
        }
    }
}
