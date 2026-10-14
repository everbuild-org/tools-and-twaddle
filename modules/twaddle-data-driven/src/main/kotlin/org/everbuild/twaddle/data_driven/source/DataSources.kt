package org.everbuild.twaddle.data_driven.source

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import net.kyori.adventure.key.Key
import net.minestom.server.event.EventNode
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.ForkJoinPool
import kotlinx.coroutines.runBlocking
import org.everbuild.twaddle.data_driven.loading.BoundSource
import org.everbuild.twaddle.data_driven.DataDriven
import org.everbuild.twaddle.data_driven.DataSnapshot
import org.everbuild.twaddle.data_driven.loading.Definition
import kotlin.collections.plusAssign

/** Coordinates source replacement for the registries declared in [block]. */
class DataSources @JvmOverloads constructor(
    private val eventNode: EventNode<in DataSourceReloadEvent>? = null,
    block: DataSourcesBuilder.() -> Unit,
) {
    private val bindings: List<RegistryBinding<*>> = DataSourcesBuilder().apply(block).targets.map { (registry, declarations) ->
        RegistryBinding(registry, declarations.toList())
    }
    private val reloadMutex = Mutex()

    /** Performs one serialized all-or-nothing load. I/O and decoding run on [Dispatchers.IO]. */
    @JvmSynthetic
    suspend fun reload(): ReloadResult = reloadMutex.withLock {
        val candidates = mutableListOf<Pair<RegistryBinding<*>, Candidate>>()
        val diagnostics = mutableListOf<ReloadDiagnostic>()
        try {
            withContext(Dispatchers.IO) {
                for (binding in bindings) {
                    val candidate = binding.load(diagnostics)
                    candidates += binding to candidate
                }
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Exception) {
            diagnostics += ReloadDiagnostic(failure.message ?: "Data reload failed", cause = failure)
            return@withLock ReloadResult.failure(diagnostics.toList())
        }

        val published = candidates.map { (binding, candidate) -> binding.publish(candidate) }
        if (eventNode != null) {
            published.forEach { (registry, snapshot) ->
                try {
                    eventNode.call(DataSourceReloadEvent(registry, snapshot))
                } catch (failure: Exception) {
                    diagnostics += ReloadDiagnostic("Snapshot published, but a reload event listener failed", cause = failure)
                }
            }
        }
        ReloadResult.success(diagnostics.toList())
    }

    /** Runs [reload] on the common ForkJoin pool and completes with its structured result. */
    fun reloadAsync(): CompletionStage<ReloadResult> = CompletableFuture.supplyAsync(
        { runBlocking { reload() } },
        ForkJoinPool.commonPool(),
    )

    /** Blocks the calling thread until the serialized reload completes. */
    fun reloadBlocking(): ReloadResult = runBlocking { reload() }
}

private data class Candidate(val entries: Map<Key, Any>)

private class RegistryBinding<T>(val registry: DataDriven<T>, private val declarations: List<Any>) {
    @Suppress("UNCHECKED_CAST")
    fun load(diagnostics: MutableList<ReloadDiagnostic>): Candidate {
        val entries = LinkedHashMap<Key, Any>()
        val provenance = HashMap<Key, String>()
        for (declaration in declarations) {
            when (declaration) {
                is Definition<*> -> put(declaration.key, declaration.value as T, declaration.origin, entries, provenance, diagnostics)
                is BoundSource<*> -> {
                    val source = declaration as BoundSource<T>
                    val resources = try {
                        source.source.discover()
                    } catch (failure: Exception) {
                        throw IOException("Failed discovering resources from ${source.source.description}", failure)
                    }
                    for (resource in resources) {
                        val key = source.pattern.key(resource.path) ?: continue
                        val value = try {
                            source.loader.load(resource)
                        } catch (failure: Exception) {
                            throw IOException("Failed loading '$key' from ${resource.origin}", failure)
                        }
                        put(key, value, resource.origin, entries, provenance, diagnostics)
                    }
                }
                else -> error("Unknown data source declaration: ${declaration::class}")
            }
        }
        return Candidate(entries)
    }

    private fun put(
        key: Key,
        value: T,
        origin: String,
        entries: MutableMap<Key, Any>,
        provenance: MutableMap<Key, String>,
        diagnostics: MutableList<ReloadDiagnostic>,
    ) {
        val previous = provenance.put(key, origin)
        if (previous != null) diagnostics += ReloadDiagnostic("'$key' from $origin overrides $previous", origin, key)
        entries[key] = value as Any
    }

    @Suppress("UNCHECKED_CAST")
    fun publish(candidate: Candidate): Pair<DataDriven<*>, DataSnapshot<*>> =
        registry to registry.publish(candidate.entries as Map<Key, T>)
}
