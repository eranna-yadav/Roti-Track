package com.rotitrack.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.rotitrack.app.rotiTrack
import kotlin.concurrent.thread

/**
 * Reminders ride on AlarmManager, not WorkManager: Android holds background work back for
 * hours while a locked phone dozes, but "allow while idle" alarms still go off on time.
 */
object Alarms {
    const val ACTION_WATER = "com.rotitrack.app.WATER_REMINDER"
    const val ACTION_MEAL = "com.rotitrack.app.MEAL_REMINDER"
    const val EXTRA_AT = "at"
    const val EXTRA_KEY = "key"

    /** Android 12+ asks the user before an app may set exact alarms ("Alarms & reminders"). */
    fun exactAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.S || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()

    fun set(context: Context, requestCode: Int, at: Long, intent: Intent) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = pending(context, requestCode, intent)
        if (exactAllowed(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            // Without the permission Android may deliver it a few minutes late, but still while locked.
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, requestCode: Int, intent: Intent) {
        context.getSystemService(AlarmManager::class.java).cancel(pending(context, requestCode, intent))
    }

    private fun pending(context: Context, requestCode: Int, intent: Intent): PendingIntent =
        PendingIntent.getBroadcast(
            context, requestCode, intent.setClass(context, ReminderAlarmReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

    /** Queues every reminder again for the signed-in user, e.g. after a restart. */
    fun rescheduleAll(context: Context) {
        val store = context.rotiTrack.activeStore ?: return
        Reminders.schedule(context, store.profile, store.prefs)
        MealReminders.schedule(context, store.prefs.mealReminders)
    }
}

/** Runs a reminder when its alarm goes off. Sound and voice take a few seconds, so it works off the main thread. */
class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext
        val result = goAsync()
        thread(name = "reminder") {
            try {
                when (intent.action) {
                    Alarms.ACTION_WATER -> runWaterReminder(app, intent.getLongExtra(Alarms.EXTRA_AT, 0L))
                    Alarms.ACTION_MEAL -> intent.getStringExtra(Alarms.EXTRA_KEY)?.let { runMealReminder(app, it) }
                }
            } finally {
                result.finish()
            }
        }
    }
}

/** Alarms are wiped when the phone restarts, the app updates or the clock changes, so set them again. */
class RescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alarms.rescheduleAll(context.applicationContext)
    }
}
