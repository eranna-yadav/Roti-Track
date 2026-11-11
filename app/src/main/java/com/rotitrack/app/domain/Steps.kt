package com.rotitrack.app.domain

import kotlin.math.roundToInt

/**
 * Daily steps from the phone's step counter sensor, which reports the total since the phone
 * last started. Each reading adds the steps taken since the previous one to today's count.
 */
object Steps {
    const val DEFAULT_GOAL = 8000

    /** More than this between two readings is a sensor glitch, not walking. */
    private const val MAX_JUMP = 50_000L

    /** Days of step history kept. */
    private const val KEEP_DAYS = 120L

    /**
     * Adds a reading of the phone's counter ([counter], steps since boot) to [steps] (day → steps).
     * [last] is the previous reading, or -1 before the first one. Returns the new map and reading.
     * The first reading only sets the starting point; after a restart the counter starts again
     * from zero, so everything it shows is new.
     */
    fun record(steps: Map<String, Int>, last: Long, counter: Long, day: String): Pair<Map<String, Int>, Long> {
        if (counter < 0) return steps to last
        val delta = when {
            last < 0 -> 0L
            counter >= last -> counter - last
            else -> counter
        }
        if (delta <= 0 || delta > MAX_JUMP) return steps to counter
        val oldest = Days.shift(day, -KEEP_DAYS)
        val updated = steps.filterKeys { it >= oldest } + (day to (steps[day] ?: 0) + delta.toInt())
        return updated to counter
    }

    /** Calories burned walking, about 0.04 kcal a step for a 70 kg person. */
    fun kcal(steps: Int, weightKg: Double): Int = (steps * weightKg * 0.00057).roundToInt()

    /** Distance in km, from a stride of about 41.5% of height. */
    fun km(steps: Int, heightCm: Int): Double = steps * heightCm * 0.415 / 100_000.0
}
