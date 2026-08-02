package org.everbuild.twaddle.testserver_java;

import net.minestom.server.Auth;
import net.minestom.server.MinecraftServer;
import org.everbuild.twaddle.core.TwaddleContext;
import org.everbuild.twaddle.core.logging.LogField;
import org.everbuild.twaddle.core.logging.StructuredLog;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class JavaTwaddleTestserver {
    private static final Logger LOGGER = LoggerFactory.getLogger(JavaTwaddleTestserver.class);

    static void main() {
        var cx = new TwaddleContext();

        try {
            var minecraftServer = MinecraftServer.init(new Auth.Online());
            StructuredLog.info(LOGGER, "Testserver binding",
                    LogField.field("auth", "online"),
                    LogField.field("port", 25565)
            );

            minecraftServer.start("0.0.0.0", 25565);
        } finally {
            cx.shutdownNow();
        }
    }
}
