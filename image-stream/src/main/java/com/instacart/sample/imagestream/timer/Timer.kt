package com.instacart.sample.imagestream.timer

import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

class Timer(
    rate: Int,
    period: Duration = 1.seconds,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    private val minDuration = period / rate
    private var lastMark: TimeMark? = null

    fun tick(block: () -> Unit): Boolean {
        val now = timeSource.markNow()
        val last = lastMark

        if (last == null) {
            lastMark = now
            block()
            return true
        }

        val elapsed = last.elapsedNow()
        if (elapsed >= minDuration) {
            lastMark = now
            block()
            return true
        }
        return false
    }

    fun reset() {
        lastMark = null
    }
}
