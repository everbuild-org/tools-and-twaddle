package org.everbuild.twaddle.core.logging

data class LogField(
    val key: String,
    val value: Any?,
) {
    companion object {
        @JvmStatic
        fun field(key: String, value: Any?): LogField =
            LogField(key, value)
    }
}

infix fun String.field(value: Any?): LogField =
    LogField(this, value)