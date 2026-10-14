package org.everbuild.twaddle.data_driven

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import net.kyori.adventure.key.Key
import java.util.Collections

/** A stable, application-owned registry whose contents are replaced as complete snapshots. */
class DataDriven<T> {
    private val state = MutableStateFlow(DataSnapshot<T>(emptyMap(), 0))

    /** Latest state. Collectors receive the current snapshot immediately and may skip intermediate ones. */
    val snapshots: StateFlow<DataSnapshot<T>> = state.asStateFlow()

    fun snapshot(): DataSnapshot<T> = state.value

    operator fun get(key: Key): T? = state.value.entries[key]

    internal fun publish(entries: Map<Key, T>): DataSnapshot<T> {
        val snapshot = DataSnapshot(
            Collections.unmodifiableMap(LinkedHashMap(entries)),
            state.value.revision + 1,
        )
        state.value = snapshot
        return snapshot
    }
}
