package org.everbuild.twaddle.data_driven.source

import net.kyori.adventure.key.Key

/** Diagnostics from a reload. Override diagnostics are informational; errors make the reload fail. */
data class ReloadDiagnostic(
    val message: String,
    val origin: String? = null,
    val key: Key? = null,
    val cause: Throwable? = null,
)