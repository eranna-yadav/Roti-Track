package com.rotitrack.app.reminders

import com.rotitrack.app.i18n.I18n
import com.rotitrack.app.i18n.t
import com.rotitrack.app.i18n.tIn
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
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.rotitrack.app.MainActivity
import com.rotitrack.app.R
import com.rotitrack.app.data.MealReminder
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.domain.Days
import com.rotitrack.app.rotiTrack
import java.time.LocalDateTime
import java.time.ZoneId

/** Daily "time to log your meal" reminders, one alarm per reminder, set again each time it fires. */
object MealReminders {
    private const val CHANNEL = "meals"
    private val KEYS = listOf("breakfast", "lunch", "snack", "dinner", "end_of_day")

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, t("Meal logging reminders"), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, reminders: List<MealReminder>?) {
        // Reminders used to be WorkManager jobs, which a dozing phone holds back.
        val wm = WorkManager.getInstance(context)
        KEYS.forEach { key ->
            wm.cancelUniqueWork("meal-$key")
            val r = reminders?.firstOrNull { it.key == key }
            if (r == null || !r.enabled) {
                Alarms.cancel(context, alarmId(key), mealIntent(key))
                return@forEach
            }
            val now = LocalDateTime.now()
            var next = now.withHour(r.hour).withMinute(r.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            Alarms.set(context, alarmId(key), next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), mealIntent(key))
        }
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        KEYS.forEach {
            wm.cancelUniqueWork("meal-$it")
            Alarms.cancel(context, alarmId(it), mealIntent(it))
        }
    }

    private fun alarmId(key: String) = 200 + KEYS.indexOf(key)

    private fun mealIntent(key: String) = Intent(Alarms.ACTION_MEAL).putExtra(Alarms.EXTRA_KEY, key)

    @SuppressLint("MissingPermission") // checked on the first line
    /** Returns false when notifications aren't allowed, so nothing was shown. */
    fun notify(context: Context, id: Int, title: String, text: String): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
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
        return true
    }
}

/** Gap between the notification sound and the voice reminder. */
private const val VOICE_DELAY_MS = 2_000L

/**
 * Nudges to log a meal, skipped when that meal (or, for End of Day, every meal) is already
 * logged, then sets tomorrow's alarm. Blocks while the voice speaks, so call it off the main thread.
 */
fun runMealReminder(context: Context, key: String) {
    val store = context.rotiTrack.activeStore ?: return
    // Tomorrow's alarm first, so a failure below can't stop the reminders.
    MealReminders.schedule(context, store.prefs.mealReminders)
    val meals = store.mealsForDay(Days.today())
    if (key == "end_of_day") {
        if (MealSlot.entries.all { s -> meals.any { it.slot == s } }) return
    } else {
        val slot = MealSlot.entries.firstOrNull { it.name.equals(key, ignoreCase = true) } ?: return
        if (meals.any { it.slot == slot }) return
    }
    val (title, text) = mealReminderText(key, I18n.lang, store.profile.name)
    val shown = MealReminders.notify(context, 100 + Math.floorMod(key.hashCode(), 100), title, text)
    if (shown && store.prefs.mealVoice && MealVoice.allowed(context)) {
        val (enTitle, enText) = mealReminderText(key, "en", store.profile.name)
        // Let the notification sound finish first so the voice is heard clearly.
        Thread.sleep(VOICE_DELAY_MS)
        MealVoice.speakAndWait(context, MealVoice.sentence(title, text), MealVoice.sentence(enTitle, enText), timeoutSeconds = 20)
    }
}

/** Kept so a reminder queued by an older version still runs once after updating. */
class MealReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        inputData.getString("key")?.let { runMealReminder(applicationContext, it) }
        return Result.success()
    }
}
