package org.everbuild.twaddle.data_driven.source

import org.everbuild.twaddle.data_driven.DataDriven
import java.util.function.Consumer

@DataSourcesDsl
class DataSourcesBuilder {
    internal val targets = LinkedHashMap<DataDriven<*>, MutableList<Any>>()

    infix fun <T> DataDriven<T>.via(block: RegistrySources<T>.() -> Unit) {
        val declarations = RegistrySources<T>().apply(block).declarations
        targets.getOrPut(this) { mutableListOf() }.addAll(declarations)
    }

    /** Java-friendly alternative to the Kotlin `registry via { ... }` DSL. */
    fun <T> addRegistry(registry: DataDriven<T>, configure: Consumer<RegistrySources<T>>) {
        val declarations = RegistrySources<T>().also(configure::accept).declarations
        targets.getOrPut(registry) { mutableListOf() }.addAll(declarations)
    }
}