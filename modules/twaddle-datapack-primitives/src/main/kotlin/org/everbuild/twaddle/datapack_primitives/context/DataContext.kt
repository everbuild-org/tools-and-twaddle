package org.everbuild.twaddle.datapack_primitives.context

class DataContext(
    private val behaviours: HashMap<DataContextKey<*>, Any?>
) {
    private val defaultBehaviours = HashMap<DataContextKey<*>, (DataContext) -> Any?>()

    constructor() : this(HashMap())

    fun <T> getBehaviour(key: DataContextKey<T>): T {
        if (behaviours.containsKey(key)) return behaviours[key].cast()
        val default = defaultBehaviours[key]
            ?: error("Missing datapack context value: $key")
        @Suppress("UNCHECKED_CAST")
        return default(this) as T
    }

    /** Returns null only when neither an explicit value nor a fallback is available. */
    fun <T> getBehaviourOrNull(key: DataContextKey<T>): T? {
        if (behaviours.containsKey(key)) return behaviours[key].cast()
        val default = defaultBehaviours[key] ?: return null
        @Suppress("UNCHECKED_CAST")
        return default(this) as T?
    }

    fun <T> withBehaviour(key: DataContextKey<T>, value: T): DataContext {
        behaviours[key] = value
        return this
    }

    /**
     * Installs a context-local fallback for [key]. Explicit values set with [withBehaviour] take
     * precedence, so a standard runtime can supply useful defaults while callers keep overrides.
     */
    fun <T> withDefaultBehaviour(key: DataContextKey<T>, default: (DataContext) -> T): DataContext {
        defaultBehaviours[key] = default
        return this
    }

    operator fun <T> get(key: DataContextKey<T>): T = getBehaviour(key)
    operator fun <T> set(key: DataContextKey<T>, value: T) {
        withBehaviour(key, value)
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> Any?.cast(): T = this as T

    companion object
}
