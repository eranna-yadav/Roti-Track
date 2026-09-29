package com.rotitrack.app.reminders

import com.rotitrack.app.i18n.t
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rotitrack.app.MainActivity
import com.rotitrack.app.R
import com.rotitrack.app.data.Prefs
import com.rotitrack.app.data.Profile
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.WaterSchedule
import com.rotitrack.app.rotiTrack
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Water reminders. Only the next reminder is ever queued, as an alarm: when it fires it
 * notifies, then queues the one after it, so a changed schedule takes effect at once.
 */
object Reminders {
    // The channel is silent: the app plays the user's chosen water sound itself, at their volume.
    private const val CHANNEL = "water_v2"
    private const val OLD_CHANNEL = "water"
    private const val WORK = "water-reminder-next"
    private const val OLD_WORK = "water-reminder"
    const val NOTIFICATION_ID = 1
    private const val ALARM_ID = 1

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(OLD_CHANNEL)
        val channel = NotificationChannel(CHANNEL, t("Water reminders"), NotificationManager.IMPORTANCE_HIGH).apply {
            description = t("Nudges to drink water through the day")
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    /** Queues the next reminder, or cancels reminders when they're off. */
    fun schedule(context: Context, p: Profile, prefs: Prefs) {
        // Reminders used to be WorkManager jobs, which a dozing phone holds back.
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(OLD_WORK)
        wm.cancelUniqueWork(WORK)
        val now = LocalDateTime.now()
        val next = if (p.remindersOn && p.onboarded) WaterSchedule.next(now, p, prefs) else null
        if (next == null) {
            cancel(context)
            return
        }
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        Alarms.set(context, ALARM_ID, at, Intent(Alarms.ACTION_WATER).putExtra(Alarms.EXTRA_AT, at))
    }

    fun cancel(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK)
        wm.cancelUniqueWork(OLD_WORK)
        Alarms.cancel(context, ALARM_ID, Intent(Alarms.ACTION_WATER))
    }

    @SuppressLint("MissingPermission") // checked on the first line
    fun notify(context: Context, total: Int, goal: Int, cupMl: Int) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU
        ) return

        val open = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val add = PendingIntent.getBroadcast(
            context, 1, Intent(context, QuickAddReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val left = (goal - total).coerceAtLeast(0)
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(t("Time for a glass of water 💧"))
            .setContentText(t("{0} of {1} ml so far — {2} ml to go.", total, goal, left))
            .setContentIntent(open)
            .addAction(0, t("+ {0} ml", cupMl), add)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }
}

/**
 * Nudges while the day's goal is unmet, with the chosen sound and buzz, then queues the next
 * reminder. Blocks until the sound ends, so call it off the main thread.
 */
fun runWaterReminder(context: Context, at: Long) {
    val store = context.rotiTrack.activeStore ?: return
    val p = store.profile
    val prefs = store.prefs
    // Skip a nudge that's long overdue, e.g. one held back while the phone was off.
    val onTime = System.currentTimeMillis() - at < 45 * 60_000L
    val total = store.waterTotal(Days.today())
    if (p.remindersOn && onTime && total < p.waterGoalMl) {
        Reminders.notify(context, total, p.waterGoalMl, p.cupMl)
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            if (prefs.vibration) WaterSounds.vibrate(context)
            if (prefs.soundOn) {
                // Stay alive until the sound finishes, or the process may be stopped mid-sound.
                val done = CountDownLatch(1)
                WaterSounds.play(context, prefs.sound, prefs.soundVolume) { done.countDown() }
                done.await(prefs.sound.seconds + 3L, TimeUnit.SECONDS)
            }
        }
    }
    Reminders.schedule(context, store.profile, store.prefs)
}

/** Kept so a reminder queued by an older version still runs once after updating. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        runWaterReminder(applicationContext, inputData.getLong("at", 0L))
        return Result.success()
    }
}
