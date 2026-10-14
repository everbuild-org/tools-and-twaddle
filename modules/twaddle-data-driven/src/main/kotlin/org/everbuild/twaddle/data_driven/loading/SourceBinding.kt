package org.everbuild.twaddle.data_driven.loading

import org.everbuild.twaddle.data_driven.resource.DataSource
import org.everbuild.twaddle.data_driven.pattern.ResourcePattern

/** A deferred source/pattern/loader declaration. Creating one performs no source I/O. */
class SourceBinding<T> internal constructor(internal val declarations: List<BoundSource<T>>) {
    operator fun plus(other: SourceBinding<T>): SourceBinding<T> = SourceBinding(declarations + other.declarations)
}

internal data class BoundSource<T>(val source: DataSource, val pattern: ResourcePattern, val loader: DataLoader<T>)

operator fun <T> DataSource.invoke(pattern: ResourcePattern, loader: DataLoader<T>): SourceBinding<T> =
    SourceBinding(listOf(BoundSource(this, pattern, loader)))

/** Creates source bindings in list order, preserving their provenance and override precedence. */
fun <T> Iterable<DataSource>.applyAll(pattern: ResourcePattern, loader: DataLoader<T>): SourceBinding<T> =
    SourceBinding(flatMap { source -> source(pattern, loader).declarations })

/** Named alternative to [invoke] for Java callers. */
fun <T> bindDataSource(source: DataSource, pattern: ResourcePattern, loader: DataLoader<T>): SourceBinding<T> = source(pattern, loader)
