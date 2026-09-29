package com.rotitrack.app.reminders

import com.rotitrack.app.i18n.tIn
import java.time.LocalDate

/**
 * Warm, varied messages for each reminder: title and text. Every title says the user's first
 * name ({0}) and the meal, so the spoken reminder always includes both.
 */
val MEAL_MESSAGES: Map<String, List<Pair<String, String>>> = mapOf(
    "breakfast" to listOf(
        "Good morning, {0}! Breakfast time ☀️" to "A warm breakfast is the best hug for your body. Tell us what you're having today 💛",
        "Rise and shine, {0}! Let's have breakfast 🌅" to "You deserve a lovely start. Log your breakfast and let's make today amazing together.",
        "Morning, dear {0}! Your breakfast is waiting 🍵" to "Don't skip breakfast — you matter too much. Tap to add what you ate.",
    ),
    "lunch" to listOf(
        "Hey {0}! Lunch time 🍛" to "You've been working so hard. Take a break, enjoy your food and log your lunch 💛",
        "{0}, your lunch thali is waiting! 🍛" to "Eat slowly and enjoy every bite. We're proud of you for taking care of yourself.",
        "Hi {0}! Hungry for lunch? 😊" to "A good lunch keeps you strong for the rest of the day. Tap to log what you ate.",
    ),
    "snack" to listOf(
        "Chai and snack time, {0}! ☕" to "A little break makes everything better. Log your evening snack when you're ready.",
        "Hey {0}, time for a light snack 🥜" to "Pick something light and tasty — you're doing great. Tap to add your snack.",
        "Time for a small snack, {0}! 🍎" to "You've come so far today. Log your snack and keep smiling 😊",
    ),
    "dinner" to listOf(
        "Dinner time, {0}! 🌙" to "You made it through the day — well done! Enjoy a light, warm dinner and log it here 💛",
        "Hey {0}, how was your day? Dinner's ready 🍲" to "Relax, eat well and let us keep track for you. Tap to log your dinner.",
        "Good evening, {0}! Time for dinner ✨" to "A happy tummy means a happy sleep. Log your dinner and rest easy.",
    ),
    "end_of_day" to listOf(
        "Well done today, {0}! Did you log your dinner? 🌙" to "Before you sleep, log anything you missed. Every small step counts — we believe in you 💛",
        "Almost bedtime, {0}! Let's log your dinner 😴" to "Take a minute to complete today's log. You're doing better than you think.",
        "Proud of you, {0}! Add today's dinner and meals 🌟" to "Wrap up your day by logging all your meals. Sweet dreams!",
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
