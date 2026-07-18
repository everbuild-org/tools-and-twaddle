package org.everbuild.twaddle

import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob

class FeatureContext(
    id: FeatureId<*>,
    parentScope: CoroutineScope
) {
    private val job = SupervisorJob(parentScope.coroutineContext[Job])
    val featureCoroutineScope: CoroutineScope =
        CoroutineScope(parentScope.coroutineContext + job + CoroutineName("FeatureScope ${id.key}"))

    fun <T : Any> get(feature: FeatureDependency<T>): T {
        TODO()
    }

    @JvmName("getExt")
    fun <T : Any> FeatureDependency<T>.get(): T = get(this)
}