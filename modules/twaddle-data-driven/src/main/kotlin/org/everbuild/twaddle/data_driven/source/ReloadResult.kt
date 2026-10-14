package org.everbuild.twaddle.data_driven.source

data class ReloadResult(val successful: Boolean, val diagnostics: List<ReloadDiagnostic>) {
    val succeeded: Boolean get() = successful

    companion object {
        @JvmStatic
        fun success(diagnostics: List<ReloadDiagnostic> = emptyList()) = ReloadResult(true, diagnostics)

        @JvmStatic
        fun failure(diagnostics: List<ReloadDiagnostic>) = ReloadResult(false, diagnostics)
    }
}