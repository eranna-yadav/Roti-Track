package com.rotitrack.app.domain

import com.rotitrack.app.data.SleepEntry
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** Sleep counts for the day you wake up on. */
object Sleep {
    /** Shorter than this isn't a sleep (a mis-tap); longer is a forgotten "I'm awake". */
    const val MIN_MINUTES = 10
    const val MAX_MINUTES = 16 * 60

    fun dayOf(end: Long, zone: ZoneId = ZoneId.systemDefault()): String = Instant.ofEpochMilli(end).atZone(zone).toLocalDate().toString()

    fun minutes(e: SleepEntry): Int = ((e.end - e.start) / 60_000).toInt()

    /** Total minutes slept that ended on [day]. */
    fun minutesFor(entries: List<SleepEntry>, day: String, zone: ZoneId = ZoneId.systemDefault()): Int =
        entries.filter { dayOf(it.end, zone) == day }.sumOf { minutes(it) }

    fun valid(start: Long, end: Long): Boolean = (end - start) / 60_000 in MIN_MINUTES..MAX_MINUTES

    /**
     * Start and end for a night that ends on [day] at the wake-up time. A bedtime later than the
     * wake-up time (10:30 PM → 6:30 AM) is the evening before.
     */
    fun night(day: String, bedHour: Int, bedMinute: Int, wakeHour: Int, wakeMinute: Int, zone: ZoneId = ZoneId.systemDefault()): Pair<Long, Long> {
        val wakeDate = LocalDate.parse(day)
        val bed = LocalTime.of(bedHour, bedMinute)
        val wake = LocalTime.of(wakeHour, wakeMinute)
        val bedDate = if (bed >= wake) wakeDate.minusDays(1) else wakeDate
        return bedDate.atTime(bed).atZone(zone).toInstant().toEpochMilli() to wakeDate.atTime(wake).atZone(zone).toInstant().toEpochMilli()
    }
}
