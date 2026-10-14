package org.everbuild.twaddle.data_driven.loading

import org.everbuild.twaddle.data_driven.resource.ClasspathSource
import org.everbuild.twaddle.data_driven.resource.FolderSource
import org.everbuild.twaddle.data_driven.pattern.ResourcePattern
import org.everbuild.twaddle.data_driven.resource.ZipSource
import java.nio.file.Path
import kotlin.reflect.KClass

/** Convenience declaration for an exploded classpath or JAR source. */
fun <T> classpath(pattern: ResourcePattern, anchor: KClass<*>, loader: DataLoader<T>): SourceBinding<T> =
    ClasspathSource(anchor)(pattern, loader)

/** Convenience declaration for a filesystem directory. */
fun <T> folder(pattern: ResourcePattern, path: Path, loader: DataLoader<T>): SourceBinding<T> =
    FolderSource(path)(pattern, loader)

/** Convenience declaration for a ZIP archive. */
fun <T> zip(pattern: ResourcePattern, path: Path, loader: DataLoader<T>): SourceBinding<T> =
    ZipSource(path)(pattern, loader)

