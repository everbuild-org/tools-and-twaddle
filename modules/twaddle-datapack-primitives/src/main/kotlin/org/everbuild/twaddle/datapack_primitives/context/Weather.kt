package org.everbuild.twaddle.datapack_primitives.context

/** Weather state visible at the current context origin. */
data class WeatherState(val raining: Boolean, val thundering: Boolean)

val WeatherContext = DataContextKey<WeatherState>("twaddle:weather_context")

@JvmSynthetic
fun DataContext.withWeather(state: WeatherState) = withBehaviour(WeatherContext, state)
