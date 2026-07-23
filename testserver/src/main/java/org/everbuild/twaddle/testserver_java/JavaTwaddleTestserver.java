package org.everbuild.twaddle.testserver_java;

import org.everbuild.twaddle.core.logging.LogField;
import org.everbuild.twaddle.core.logging.StructuredLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JavaTwaddleTestserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaTwaddleTestserver.class);

    static void main() {
        StructuredLog.info(LOGGER, "Hello, world!", LogField.field("abc", "def"));
    }
}
