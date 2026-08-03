@file:JvmName("KotlinMdc")
package org.everbuild.twaddle.core.logging

import org.slf4j.Logger
import org.slf4j.event.Level
import org.slf4j.spi.LoggingEventBuilder

@JvmSynthetic
inline fun Logger.log(
    level: Level,
    vararg fields: LogField,
    throwable: Throwable? = null,
    message: () -> String,
) {
    if (!isEnabledForLevel(level)) return

    atLevel(level)
        .applyFields(fields)
        .apply {
            if (throwable != null) {
                setCause(throwable)
            }
        }
        .log(message())
}

@JvmSynthetic
@PublishedApi
internal fun LoggingEventBuilder.applyFields(
    fields: Array<out LogField>,
): LoggingEventBuilder = apply {
    fields.forEach { (key, value) ->
        addKeyValue(key, value)
    }
}

@JvmSynthetic
inline fun Logger.trace(
    vararg fields: LogField,
    throwable: Throwable? = null,
    message: () -> String,
) = log(
    level = Level.TRACE,
    fields = fields,
    throwable = throwable,
    message = message,
)

@JvmSynthetic
inline fun Logger.debug(
    vararg fields: LogField,
    throwable: Throwable? = null,
    message: () -> String,
) = log(
    level = Level.DEBUG,
    fields = fields,
    throwable = throwable,
    message = message,
)

@JvmSynthetic
inline fun Logger.info(
    vararg fields: LogField,
    throwable: Throwable? = null,
    message: () -> String,
) = log(
    level = Level.INFO,
    fields = fields,
    throwable = throwable,
    message = message,
)

@JvmSynthetic
inline fun Logger.warn(
    vararg fields: LogField,
    throwable: Throwable? = null,
    message: () -> String,
) = log(
    level = Level.WARN,
    fields = fields,
    throwable = throwable,
    message = message,
)

@JvmSynthetic
inline fun Logger.error(
    vararg fields: LogField,
    throwable: Throwable? = null,
    message: () -> String,
) = log(
    level = Level.ERROR,
    fields = fields,
    throwable = throwable,
    message = message,
)