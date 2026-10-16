package org.everbuild.twaddle.datapack_primitives.number_provider.context

class NumberProviderContext(
    private val behaviours: HashMap<NumberProviderContextKey<*>, Any?>
) {
    fun <T> getBehaviour(key: NumberProviderContextKey<T>): T {
        check(behaviours.containsKey(key)) {
            "Missing number provider context value: $key"
        }
        @Suppress("UNCHECKED_CAST")
        return behaviours[key] as T
    }

    fun <T> withBehaviour(key: NumberProviderContextKey<T>, value: T): NumberProviderContext {
        behaviours[key] = value
        return this
    }

    operator fun <T> get(key: NumberProviderContextKey<T>): T? = getBehaviour(key)
    operator fun <T> set(key: NumberProviderContextKey<T>, value: T) {
        withBehaviour(key, value)
    }
}