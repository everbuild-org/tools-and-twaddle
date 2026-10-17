package org.everbuild.twaddle.data_driven.source

import net.kyori.adventure.key.Key
import org.everbuild.twaddle.data_driven.loading.DataLoader
import org.everbuild.twaddle.data_driven.resource.DataSource
import org.everbuild.twaddle.data_driven.loading.Definition
import org.everbuild.twaddle.data_driven.loading.RegistryDeclaration
import org.everbuild.twaddle.data_driven.pattern.ResourcePattern
import org.everbuild.twaddle.data_driven.loading.SourceBinding
import org.everbuild.twaddle.data_driven.loading.classpath
import org.everbuild.twaddle.data_driven.loading.folder
import org.everbuild.twaddle.data_driven.loading.invoke
import org.everbuild.twaddle.data_driven.loading.zip
import java.nio.file.Path
import kotlin.collections.plusAssign
import kotlin.reflect.KClass

@DataSourcesDsl
class RegistrySources<T : Any> internal constructor() {
    internal val declarations = mutableListOf<RegistryDeclaration<T>>()

    operator fun Pair<String, T>.unaryPlus() {
        addStatic(first, second)
    }

    fun addStatic(identifier: String, value: T): RegistrySources<T> {
        val key = try { Key.key(identifier) } catch (failure: RuntimeException) {
            throw IllegalArgumentException("Invalid static definition key '$identifier'", failure)
        }
        declarations += Definition(key, value, "static:$identifier")
        return this
    }

    operator fun SourceBinding<T>.unaryPlus() {
        addBinding(this)
    }

    fun addBinding(binding: SourceBinding<T>): RegistrySources<T> {
        declarations.addAll(binding.declarations)
        return this
    }

    fun addSource(source: DataSource, pattern: ResourcePattern, loader: DataLoader<T>): RegistrySources<T> =
        addBinding(source(pattern, loader))

    fun addClasspath(pattern: ResourcePattern, anchor: KClass<*>, loader: DataLoader<T>): RegistrySources<T> =
        addBinding(classpath(pattern, anchor, loader))

    fun addFolder(pattern: ResourcePattern, path: Path, loader: DataLoader<T>): RegistrySources<T> =
        addBinding(folder(pattern, path, loader))

    fun addZip(pattern: ResourcePattern, path: Path, loader: DataLoader<T>): RegistrySources<T> =
        addBinding(zip(pattern, path, loader))
}
