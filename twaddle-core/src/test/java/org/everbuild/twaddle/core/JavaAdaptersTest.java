package org.everbuild.twaddle.core;

import net.minestom.server.MinecraftServer;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class JavaAdaptersTest {
    @Test
    void phasedModuleCanBePreparedInstalledAndUninstalled() {
        var cleaned = new CompletableFuture<Void>();
        var prepared = FoundryModules.prepareModuleAsync(
                JavaAdapterFixtures.phasedModule("ready", cleaned)
        ).toCompletableFuture().join();

        assertFalse(cleaned.isDone());

        var installed = prepared.installAsync(new MinecraftServer())
                .toCompletableFuture().join();

        assertEquals("ready", installed.getApi());
        assertFalse(cleaned.isDone());

        CompletionStage<Void> uninstalled = installed.uninstallAsync();
        uninstalled.toCompletableFuture().join();
        assertTrue(cleaned.isDone());
    }

    @Test
    void preparedModuleCanBeDiscardedRepeatedly() {
        var cleaned = new CompletableFuture<Void>();
        var prepared = FoundryModules.prepareModuleAsync(
                JavaAdapterFixtures.phasedModule("unused", cleaned)
        ).toCompletableFuture().join();

        prepared.discardAsync().toCompletableFuture().join();
        prepared.discardAsync().toCompletableFuture().join();

        assertTrue(cleaned.isDone());
    }

    @Test
    void directModuleCanBeInstalledAndUninstalled() {
        var cleaned = new CompletableFuture<Void>();
        var installed = FoundryModules.installModuleAsync(
                JavaAdapterFixtures.directModule("ready", cleaned),
                new MinecraftServer()
        ).toCompletableFuture().join();

        assertEquals("ready", installed.getApi());
        assertFalse(cleaned.isDone());

        installed.uninstallAsync().toCompletableFuture().join();
        assertTrue(cleaned.isDone());
    }

    @Test
    void preparationFailureRetainsItsCause() {
        var failure = new IllegalStateException("preparation failed");
        var stage = FoundryModules.prepareModuleAsync(
                JavaAdapterFixtures.failingPhasedModule(failure)
        ).toCompletableFuture();

        var thrown = assertThrows(CompletionException.class, stage::join);

        var cause = assertInstanceOf(IllegalStateException.class, thrown.getCause());
        assertEquals(failure.getMessage(), cause.getMessage());
    }

    // Retains compile-time coverage for the high-level handle adapter without
    // initializing and binding Minestom in this unit test suite.
    static void stop(RunningFoundry foundry) {
        foundry.stopAsync();
    }
}
