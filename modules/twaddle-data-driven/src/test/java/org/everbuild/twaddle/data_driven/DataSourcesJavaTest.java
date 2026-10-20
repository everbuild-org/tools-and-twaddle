package org.everbuild.twaddle.data_driven;

import net.kyori.adventure.key.Key;
import org.everbuild.twaddle.data_driven.source.DataSources;
import org.everbuild.twaddle.data_driven.source.ReloadResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataSourcesJavaTest {
    @Test
    void javaCanCreateARegistryConfigureStaticDefinitionsAndReloadAsync() throws Exception {
        DataDriven<String> registry = new DataDriven<>();
        DataSources sources = DataSources.create(builder ->
                builder.addRegistry(registry, definitions ->
                        definitions.addStatic("java:example", "loaded")
                )
        );

        ReloadResult result = sources.reloadAsync().toCompletableFuture().get(5, TimeUnit.SECONDS);

        assertTrue(result.getSuccessful());
        assertEquals("loaded", registry.get(Key.key("java:example")));
        assertEquals(1L, registry.snapshot().getRevision());
    }
}
