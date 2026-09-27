package com.rotitrack.app

import com.rotitrack.app.data.DEFAULT_CUSTOM_TIMES
import com.rotitrack.app.data.Prefs
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.ReminderTime
import com.rotitrack.app.data.WaterReminderMode
import com.rotitrack.app.domain.WaterSchedule
import com.rotitrack.app.store.AppStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime

class WaterScheduleTest {
    private val profile = Profile(onboarded = true, remindersOn = true, wakeHour = 7, sleepHour = 22, reminderEveryMin = 90)
    // 2026-09-25 is a Friday.
    private val friday9am = LocalDateTime.of(2026, 9, 25, 9, 0)

    @Test fun standardSkipsDisabledTimes() {
        val prefs = Prefs()
        val times = WaterSchedule.times(profile, prefs.copy(standardTimes = prefs.standardTimes.mapIndexed { i, t -> t.copy(enabled = i != 2) }))
        assertEquals(7, times.size)
        assertEquals(6 * 60 + 30, times.first())
    }

    @Test fun nextIsLaterToday() {
        assertEquals(LocalDateTime.of(2026, 9, 25, 9, 30), WaterSchedule.next(friday9am, profile, Prefs()))
    }

    @Test fun afterTheLastOneItRollsToTomorrow() {
        val late = LocalDateTime.of(2026, 9, 25, 22, 30)
        assertEquals(LocalDateTime.of(2026, 9, 26, 6, 30), WaterSchedule.next(late, profile, Prefs()))
    }

    @Test fun weekendModeSkipsToMonday() {
        val late = LocalDateTime.of(2026, 9, 25, 22, 30)
        assertEquals(LocalDateTime.of(2026, 9, 28, 6, 30), WaterSchedule.next(late, profile, Prefs(weekendMode = true)))
    }

    @Test fun intervalRunsFromWakeToBedtime() {
        val times = WaterSchedule.intervalTimes(7, 22, 90)
        assertEquals(8 * 60 + 30, times.first())
        assertEquals(22 * 60, times.last())
        assertEquals(10, times.size)
        val prefs = Prefs(waterReminderMode = WaterReminderMode.INTERVAL)
        assertEquals(LocalDateTime.of(2026, 9, 25, 10, 0), WaterSchedule.next(friday9am, profile, prefs))
    }

    @Test fun customUsesOnlyEnabledTimes() {
        val prefs = Prefs(
            waterReminderMode = WaterReminderMode.CUSTOM,
            customTimes = listOf(ReminderTime(9, 0), ReminderTime(11, 15, enabled = false), ReminderTime(16, 45)),
        )
        assertEquals(LocalDateTime.of(2026, 9, 25, 16, 45), WaterSchedule.next(friday9am, profile, prefs))
        assertEquals(10, DEFAULT_CUSTOM_TIMES.count { it.enabled })
    }

    @Test fun noTimesMeansNoReminder() {
        val prefs = Prefs(waterReminderMode = WaterReminderMode.CUSTOM, customTimes = emptyList())
        assertNull(WaterSchedule.next(friday9am, profile, prefs))
    }

    @Test fun oldSavesLoadWithDefaults() {
        val s = AppStore.decode("""{"profile":{"onboarded":true},"prefs":{"language":"en"}}""")
        assertEquals(WaterReminderMode.STANDARD, s.prefs.waterReminderMode)
        assertEquals(8, s.prefs.standardTimes.size)
        assertEquals(true, s.prefs.soundOn)
    }
}
