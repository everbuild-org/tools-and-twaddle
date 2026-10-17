package org.everbuild.twaddle.datapack_primitives.context

class DataContext(
    private val behaviours: HashMap<DataContextKey<*>, Any?>
) {
    fun <T> getBehaviour(key: DataContextKey<T>): T {
        check(behaviours.containsKey(key)) {
            "Missing number provider context value: $key"
        }
        @Suppress("UNCHECKED_CAST")
        return behaviours[key] as T
    }

    fun <T> withBehaviour(key: DataContextKey<T>, value: T): DataContext {
        behaviours[key] = value
        return this
    }

    operator fun <T> get(key: DataContextKey<T>): T = getBehaviour(key)
    operator fun <T> set(key: DataContextKey<T>, value: T) {
        withBehaviour(key, value)
    }
}