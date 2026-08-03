package org.everbuild.twaddle.core.logging

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.lang.invoke.MethodHandles

@Suppress("NOTHING_TO_INLINE")
inline fun logger(): Logger =
    LoggerFactory.getLogger(MethodHandles.lookup().lookupClass())

inline fun <reified T : Any> loggerFor(): Logger =
    LoggerFactory.getLogger(T::class.java)

fun logger(name: String): Logger =
    LoggerFactory.getLogger(name)