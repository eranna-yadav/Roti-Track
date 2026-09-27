package com.rotitrack.app.domain

import com.rotitrack.app.data.Prefs
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.WaterReminderMode
import java.time.DayOfWeek
import java.time.LocalDateTime

/** When water reminders go off, for whichever mode the user picked. */
object WaterSchedule {

    /** Minutes after midnight of each reminder in a day, sorted and without repeats. */
    fun times(profile: Profile, prefs: Prefs): List<Int> = when (prefs.waterReminderMode) {
        WaterReminderMode.STANDARD -> prefs.standardTimes.filter { it.enabled }.map { it.minuteOfDay }
        WaterReminderMode.CUSTOM -> prefs.customTimes.filter { it.enabled }.map { it.minuteOfDay }
        WaterReminderMode.INTERVAL -> intervalTimes(profile.wakeHour, profile.sleepHour, profile.reminderEveryMin)
    }.distinct().sorted()

    /** From [wakeHour] up to [sleepHour], every [everyMin] minutes (the first one [everyMin] after waking). */
    fun intervalTimes(wakeHour: Int, sleepHour: Int, everyMin: Int): List<Int> {
        val step = everyMin.coerceAtLeast(15)
        return generateSequence(wakeHour * 60 + step) { it + step }.takeWhile { it <= sleepHour * 60 }.toList()
    }

    /** The first reminder strictly after [now], or null when there are none. */
    fun next(now: LocalDateTime, profile: Profile, prefs: Prefs): LocalDateTime? {
        val times = times(profile, prefs)
        if (times.isEmpty()) return null
        val nowMin = now.hour * 60 + now.minute
        for (dayOffset in 0L..7L) {
            val date = now.toLocalDate().plusDays(dayOffset)
            if (prefs.weekendMode && isWeekend(date.dayOfWeek)) continue
            val t = times.firstOrNull { dayOffset > 0 || it > nowMin } ?: continue
            return date.atTime(t / 60, t % 60)
        }
        return null
    }

    fun isWeekend(d: DayOfWeek) = d == DayOfWeek.SATURDAY || d == DayOfWeek.SUNDAY
}
