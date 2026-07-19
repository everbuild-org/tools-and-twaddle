@file:JvmName("JavaAdapterFixtures")

package org.everbuild.twaddle.core

import java.util.concurrent.CompletableFuture

fun directModule(
    api: String,
    cleaned: CompletableFuture<Void>,
): DirectModule<String> = directModule {
    try {
        installed { api }
    } finally {
        cleaned.complete(null)
    }
}

fun phasedModule(
    api: String,
    cleaned: CompletableFuture<Void>,
): PhasedModule<String> = phasedModule {
    try {
        afterInit { api }
    } finally {
        cleaned.complete(null)
    }
}

fun failingPhasedModule(
    failure: RuntimeException,
): PhasedModule<String> = phasedModule {
    throw failure
}
