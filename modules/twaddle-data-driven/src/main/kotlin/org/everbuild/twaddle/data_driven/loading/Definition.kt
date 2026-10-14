package org.everbuild.twaddle.data_driven.loading

import net.kyori.adventure.key.Key

/** A typed declaration that contributes definitions to a registry during reload. */
internal sealed interface RegistryDeclaration<out T>

internal data class Definition<T>(val key: Key, val value: T, val origin: String) : RegistryDeclaration<T>
