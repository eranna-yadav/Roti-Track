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
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.rotitrack.app.MainActivity
import com.rotitrack.app.R
import com.rotitrack.app.data.MealReminder
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.domain.Days
import com.rotitrack.app.rotiTrack
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/** Daily "time to log your meal" reminders, one periodic job per reminder. */
object MealReminders {
    private const val CHANNEL = "meals"
    private val KEYS = listOf("breakfast", "lunch", "snack", "dinner", "end_of_day")

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, t("Meal logging reminders"), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, reminders: List<MealReminder>?) {
        val wm = WorkManager.getInstance(context)
        KEYS.forEach { key ->
            val r = reminders?.firstOrNull { it.key == key }
            if (r == null || !r.enabled) {
                wm.cancelUniqueWork("meal-$key")
                return@forEach
            }
            val now = LocalDateTime.now()
            var next = now.withHour(r.hour).withMinute(r.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            val request = PeriodicWorkRequestBuilder<MealReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(Duration.between(now, next).toMillis(), TimeUnit.MILLISECONDS)
                .setInputData(workDataOf("key" to key))
                .build()
            // Replace so a changed time takes effect straight away.
            wm.enqueueUniquePeriodicWork("meal-$key", ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request)
        }
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        KEYS.forEach { wm.cancelUniqueWork("meal-$it") }
    }

    @SuppressLint("MissingPermission") // checked on the first line
    fun notify(context: Context, id: Int, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }
}

/** Skips the nudge when that meal (or, for End of Day, every meal) is already logged. */
class MealReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        val store = applicationContext.rotiTrack.activeStore ?: return Result.success()
        val key = inputData.getString("key") ?: return Result.success()
        val meals = store.mealsForDay(Days.today())
        val (title, text) = when (key) {
            "end_of_day" -> {
                if (MealSlot.entries.all { s -> meals.any { it.slot == s } }) return Result.success()
                t("Wrap up your day 🌙") to t("Log everything you ate today in one go.")
            }
            else -> {
                val slot = MealSlot.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: return Result.success()
                if (meals.any { it.slot == slot }) return Result.success()
                t("Time to log your {0} {1}", slot.short.lowercase(), slot.emoji) to t("Tap to add what you ate. It takes a few seconds.")
            }
        }
        MealReminders.notify(applicationContext, 100 + Math.floorMod(key.hashCode(), 100), title, text)
        return Result.success()
    }
}
