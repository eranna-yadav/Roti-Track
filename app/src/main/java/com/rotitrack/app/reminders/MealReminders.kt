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
import java.time.LocalDate
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

/** Warm, varied messages for each reminder: title ({0} is the first name) and text. */
val MEAL_MESSAGES: Map<String, List<Pair<String, String>>> = mapOf(
    "breakfast" to listOf(
        "Good morning, {0}! ☀️" to "A warm breakfast is the best hug for your body. Tell us what you're having today 💛",
        "Rise and shine, {0}! 🌅" to "You deserve a lovely start. Log your breakfast and let's make today amazing together.",
        "Morning, dear {0}! 🍵" to "Don't skip breakfast — you matter too much. Tap to add what you ate.",
    ),
    "lunch" to listOf(
        "Hey {0}! Lunch time 🍛" to "You've been working so hard. Take a break, enjoy your food and log your lunch 💛",
        "{0}, your thali is waiting! 🍛" to "Eat slowly and enjoy every bite. We're proud of you for taking care of yourself.",
        "Hi {0}! Hungry? 😊" to "A good lunch keeps you strong for the rest of the day. Tap to log what you ate.",
    ),
    "snack" to listOf(
        "Chai time, {0}! ☕" to "A little break makes everything better. Log your evening snack when you're ready.",
        "Hey {0}, feeling peckish? 🥜" to "Pick something light and tasty — you're doing great. Tap to add your snack.",
        "Time for a small treat, {0}! 🍎" to "You've come so far today. Log your snack and keep smiling 😊",
    ),
    "dinner" to listOf(
        "Dinner time, {0}! 🌙" to "You made it through the day — well done! Enjoy a light, warm dinner and log it here 💛",
        "Hey {0}, how was your day? 🍲" to "Relax, eat well and let us keep track for you. Tap to log your dinner.",
        "Good evening, {0}! ✨" to "A happy tummy means a happy sleep. Log your dinner and rest easy.",
    ),
    "end_of_day" to listOf(
        "Well done today, {0}! 🌙" to "Before you sleep, log anything you missed. Every small step counts — we believe in you 💛",
        "Almost bedtime, {0}! 😴" to "Take a minute to complete today's log. You're doing better than you think.",
        "Proud of you, {0}! 🌟" to "Wrap up your day by logging all your meals. Sweet dreams!",
    ),
)

/**
 * Title and text of a meal reminder in the language [code], greeting [fullName] by first name
 * ("friend" when unknown). [variant] picks one of the messages; by default it changes every day.
 */
fun mealReminderText(
    key: String,
    code: String,
    fullName: String = "",
    variant: Int = LocalDate.now().dayOfYear,
): Pair<String, String> {
    val messages = MEAL_MESSAGES[key] ?: MEAL_MESSAGES.getValue("end_of_day")
    val (title, text) = messages[Math.floorMod(variant, messages.size)]
    val name = fullName.trim().substringBefore(' ').ifEmpty { tIn(code, "friend") }
    return tIn(code, title, name) to tIn(code, text)
}

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
