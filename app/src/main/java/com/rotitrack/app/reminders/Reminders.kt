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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rotitrack.app.MainActivity
import com.rotitrack.app.R
import com.rotitrack.app.data.Profile
import com.rotitrack.app.domain.Days
import com.rotitrack.app.rotiTrack
import java.time.LocalTime
import java.util.concurrent.TimeUnit

object Reminders {
    private const val CHANNEL = "water"
    private const val WORK = "water-reminder"
    const val NOTIFICATION_ID = 1

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, "Water reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Nudges to drink water through the day"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    /** (Re)schedules the periodic check, or cancels it when reminders are off. */
    fun schedule(context: Context, p: Profile) {
        val wm = WorkManager.getInstance(context)
        if (!p.remindersOn || !p.onboarded) {
            wm.cancelUniqueWork(WORK)
            return
        }
        val every = p.reminderEveryMin.toLong().coerceAtLeast(15)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(every, TimeUnit.MINUTES)
            .setInitialDelay(every, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniquePeriodicWork(WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK)
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

/** Fires every interval; only nudges inside waking hours and while the goal is unmet. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = applicationContext.rotiTrack.activeStore ?: return Result.success()
        val p = store.profile
        if (!p.remindersOn) return Result.success()
        val hour = LocalTime.now().hour
        if (hour < p.wakeHour || hour >= p.sleepHour) return Result.success()
        val total = store.waterTotal(Days.today())
        if (total < p.waterGoalMl) Reminders.notify(applicationContext, total, p.waterGoalMl, p.cupMl)
        return Result.success()
    }
}
