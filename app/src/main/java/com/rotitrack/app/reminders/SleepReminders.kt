package com.rotitrack.app.reminders

import com.rotitrack.app.i18n.t
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.rotitrack.app.MainActivity
import com.rotitrack.app.R
import com.rotitrack.app.data.MealReminder
import com.rotitrack.app.domain.Days
import com.rotitrack.app.rotiTrack
import com.rotitrack.app.ui.sleepDuration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Bedtime and wake-up reminders from the Sleep screen. One alarm each, set again when it fires.
 * The buttons on them start and stop the sleep timer without opening the app.
 */
object SleepReminders {
    private const val CHANNEL = "sleep"
    private val KEYS = listOf("bedtime", "wake")
    const val NOTIFICATION_ID = 300
    const val ACTION_START = "com.rotitrack.app.SLEEP_START"
    const val ACTION_END = "com.rotitrack.app.SLEEP_END"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(CHANNEL, t("Sleep reminders"), NotificationManager.IMPORTANCE_HIGH).apply {
            description = t("Bedtime and wake-up reminders")
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    fun schedule(context: Context, reminders: List<MealReminder>?) {
        KEYS.forEach { key ->
            val r = reminders?.firstOrNull { it.key == key }
            if (r == null || !r.enabled) {
                Alarms.cancel(context, alarmId(key), intent(key))
                return@forEach
            }
            val now = LocalDateTime.now()
            var next = now.withHour(r.hour).withMinute(r.minute).withSecond(0).withNano(0)
            if (!next.isAfter(now)) next = next.plusDays(1)
            Alarms.set(context, alarmId(key), next.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(), intent(key))
        }
    }

    fun cancelAll(context: Context) {
        KEYS.forEach { Alarms.cancel(context, alarmId(it), intent(it)) }
        NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID)
    }

    private fun alarmId(key: String) = 300 + KEYS.indexOf(key)

    private fun intent(key: String) = Intent(Alarms.ACTION_SLEEP).putExtra(Alarms.EXTRA_KEY, key)

    @SuppressLint("MissingPermission") // checked on the first line
    fun notify(context: Context, title: String, text: String, button: Pair<String, String>?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val open = PendingIntent.getActivity(
            context, NOTIFICATION_ID, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val builder = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
        button?.let { (label, action) ->
            val pi = PendingIntent.getBroadcast(
                context, NOTIFICATION_ID, Intent(context, SleepActionReceiver::class.java).setAction(action),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.addAction(0, label, pi)
        }
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }
}

/**
 * Bedtime: skipped if the sleep timer is already running. Wake-up: offers "I'm awake" while it
 * runs, asks to log the night if nothing is logged yet, and stays quiet otherwise.
 */
fun runSleepReminder(context: Context, key: String) {
    val store = context.rotiTrack.activeStore ?: return
    // Tomorrow's alarm first, so a failure below can't stop the reminders.
    SleepReminders.schedule(context, store.prefs.sleepReminders)
    val sleeping = store.state.sleep.activeStart != null
    when (key) {
        "bedtime" -> if (!sleeping) SleepReminders.notify(
            context,
            t("Time for bed 🌙"),
            t("Put the phone away and aim for {0} of sleep. Tap Going to sleep when you lie down.", sleepDuration(store.prefs.sleepGoalMin)),
            t("🌙 Going to sleep") to SleepReminders.ACTION_START,
        )
        "wake" -> when {
            sleeping -> SleepReminders.notify(
                context, t("Good morning ☀️"), t("Tap I'm awake to save last night's sleep."),
                t("☀️ I'm awake") to SleepReminders.ACTION_END,
            )
            store.sleepMinutes(Days.today()) == 0 -> SleepReminders.notify(
                context, t("Good morning ☀️"), t("How did you sleep? Log last night in Roti Track."), null,
            )
        }
    }
}

/** The buttons on a sleep reminder: start or stop the sleep timer. */
class SleepActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = context.rotiTrack.activeStore ?: return
        when (intent.action) {
            SleepReminders.ACTION_START -> if (store.state.sleep.activeStart == null) store.startSleep()
            SleepReminders.ACTION_END -> store.endSleep()
        }
        NotificationManagerCompat.from(context).cancel(SleepReminders.NOTIFICATION_ID)
    }
}
