package org.everbuild.twaddle.stacked_behaviour

@JvmSynthetic
inline fun <reified T : Any> Behaviour<*>.configuration(): T? = configuration(T::class.java)

@JvmSynthetic
inline fun <reified T : Any> BehaviourEditor<*>.configuration(): T? = configuration(T::class.java)
