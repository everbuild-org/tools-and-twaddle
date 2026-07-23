package org.everbuild.twaddle.core.logging

import org.slf4j.Logger
import org.slf4j.event.Level
import java.util.function.Supplier

object StructuredLog {
    @JvmStatic
    fun log(
        logger: Logger,
        level: Level,
        message: Supplier<String>,
        vararg fields: LogField,
    ) {
        log(logger, level, null, message, *fields)
    }

    @JvmStatic
    fun log(
        logger: Logger,
        level: Level,
        throwable: Throwable?,
        message: Supplier<String>,
        vararg fields: LogField,
    ) {
        logger.log(
            level = level,
            fields = fields,
            throwable = throwable,
            message = { message.get() }
        )
    }

    @JvmStatic
    fun log(
        logger: Logger,
        level: Level,
        throwable: Throwable?,
        message: String,
        vararg fields: LogField,
    ) {
        logger.log(
            level = level,
            fields = fields,
            throwable = throwable,
            message = { message }
        )
    }

    @JvmStatic
    fun info(
        logger: Logger,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.INFO, message, *fields)

    @JvmStatic
    fun info(
        logger: Logger,
        throwable: Throwable,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.INFO, throwable, message, *fields)

    @JvmStatic
    fun info(
        logger: Logger,
        throwable: Throwable,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.INFO,
        throwable = throwable,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun info(
        logger: Logger,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.INFO,
        throwable = null,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun warn(
        logger: Logger,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.WARN, message, *fields)

    @JvmStatic
    fun warn(
        logger: Logger,
        throwable: Throwable,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.WARN, throwable, message, *fields)

    @JvmStatic
    fun warn(
        logger: Logger,
        throwable: Throwable,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.WARN,
        throwable = throwable,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun warn(
        logger: Logger,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.WARN,
        throwable = null,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun error(
        logger: Logger,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.ERROR, message, *fields)

    @JvmStatic
    fun error(
        logger: Logger,
        throwable: Throwable,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.ERROR, throwable, message, *fields)

    @JvmStatic
    fun error(
        logger: Logger,
        throwable: Throwable,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.ERROR,
        throwable = throwable,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun error(
        logger: Logger,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.ERROR,
        throwable = null,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun debug(
        logger: Logger,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.DEBUG, message, *fields)

    @JvmStatic
    fun debug(
        logger: Logger,
        throwable: Throwable,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.DEBUG,
        throwable = throwable,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun debug(
        logger: Logger,
        throwable: Throwable,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.DEBUG,
        throwable = throwable,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun debug(
        logger: Logger,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.DEBUG,
        throwable = null,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun trace(
        logger: Logger,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(logger, Level.TRACE, message, *fields)

    @JvmStatic
    fun trace(
        logger: Logger,
        throwable: Throwable,
        message: Supplier<String>,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.TRACE,
        throwable = throwable,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun trace(
        logger: Logger,
        throwable: Throwable,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.TRACE,
        throwable = throwable,
        message = message,
        fields = fields,
    )

    @JvmStatic
    fun trace(
        logger: Logger,
        message: String,
        vararg fields: LogField,
    ) = log(
        logger = logger,
        level = Level.TRACE,
        throwable = null,
        message = message,
        fields = fields,
    )
}