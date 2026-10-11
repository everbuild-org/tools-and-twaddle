package org.everbuild.twaddle.random_ticking

import net.minestom.server.instance.Instance
import net.minestom.server.tag.Tag

val RANDOM_TICK_SPEED: Tag<Int> = Tag.Integer("twaddle:random_tick_speed")
    .defaultValue(3)

var Instance.randomTickSpeed: Int
    get() = getTag(RANDOM_TICK_SPEED) ?: 3
    set(value) {
        require(value >= 0)
        setTag(RANDOM_TICK_SPEED, value)
    }