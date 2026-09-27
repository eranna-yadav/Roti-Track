package com.rotitrack.app.reminders

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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
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
 * Water reminders. Only the next reminder is ever queued: when it fires it notifies,
 * then queues the one after it, so a changed schedule takes effect at once.
 */
object Reminders {
    // The channel is silent: the app plays the user's chosen water sound itself, at their volume.
    private const val CHANNEL = "water_v2"
    private const val OLD_CHANNEL = "water"
    private const val WORK = "water-reminder-next"
    private const val OLD_WORK = "water-reminder"
    const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.deleteNotificationChannel(OLD_CHANNEL)
        val channel = NotificationChannel(CHANNEL, "Water reminders", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Nudges to drink water through the day"
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannel(channel)
    }

    /** Queues the next reminder, or cancels reminders when they're off. */
    fun schedule(context: Context, p: Profile, prefs: Prefs) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(OLD_WORK)
        val now = LocalDateTime.now()
        val next = if (p.remindersOn && p.onboarded) WaterSchedule.next(now, p, prefs) else null
        if (next == null) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val at = next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay((at - System.currentTimeMillis()).coerceAtLeast(0), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf("at" to at))
            .build()
        wm.enqueueUniqueWork(WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK)
        wm.cancelUniqueWork(OLD_WORK)
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
            .setContentTitle("Time for a glass of water 💧")
            .setContentText("$total of $goal ml so far — $left ml to go.")
            .setContentIntent(open)
            .addAction(0, "+ $cupMl ml", add)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, n)
    }
}

/** Nudges while the day's goal is unmet, with the chosen sound and buzz, then queues the next reminder. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = applicationContext.rotiTrack.activeStore ?: return Result.success()
        val p = store.profile
        val prefs = store.prefs
        val at = inputData.getLong("at", 0L)
        // Android may hold background work back while the phone dozes; skip a nudge that's long overdue.
        val onTime = System.currentTimeMillis() - at < 45 * 60_000L
        val total = store.waterTotal(Days.today())
        if (p.remindersOn && onTime && total < p.waterGoalMl) {
            Reminders.notify(applicationContext, total, p.waterGoalMl, p.cupMl)
            if (NotificationManagerCompat.from(applicationContext).areNotificationsEnabled()) {
                if (prefs.vibration) WaterSounds.vibrate(applicationContext)
                if (prefs.soundOn) {
                    // Stay alive until the sound finishes, or the process may be stopped mid-sound.
                    val done = CountDownLatch(1)
                    WaterSounds.play(applicationContext, prefs.sound, prefs.soundVolume) { done.countDown() }
                    done.await(prefs.sound.seconds + 3L, TimeUnit.SECONDS)
                }
            }
        }
        Reminders.schedule(applicationContext, store.profile, store.prefs)
        return Result.success()
    }
}
