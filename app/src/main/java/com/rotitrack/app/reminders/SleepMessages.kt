package com.rotitrack.app.reminders

import com.rotitrack.app.i18n.tIn
import java.time.LocalDate

/**
 * Gentle, uplifting words for the bedtime and wake-up reminders, spoken aloud when voice is on.
 * Every title greets the user by first name ({0}).
 */
val SLEEP_MESSAGES: Map<String, List<Pair<String, String>>> = mapOf(
    "bedtime" to listOf(
        "Good night, {0} 🌙" to "You did beautifully today. Put the phone down, take a slow deep breath, and let your body rest. Sweet dreams 💛",
        "Time to rest, {0} ✨" to "The stars are out and the day's work is done. Close your eyes and let tomorrow take care of itself.",
        "Sleep well, {0} 😴" to "Every hour of good sleep helps your body heal and brings your goals closer. Relax your shoulders and drift away.",
        "{0}, it's bedtime 🌙" to "Let go of today's worries. You are doing better than you think. Rest well.",
        "Sweet dreams, {0} 🌟" to "A calm mind and a cosy bed are waiting for you. Breathe in peace, breathe out the day.",
    ),
    "wake" to listOf(
        "Good morning, {0} ☀️" to "A brand new day is here, full of fresh chances. Stretch, smile and drink a glass of water.",
        "Rise and shine, {0} 🌅" to "The sun is up and so are you! Take a deep breath, today is going to be a lovely day.",
        "Hello sunshine, {0} 🌼" to "You rested well, and now it's your time to shine. Open the window and say hello to the morning.",
        "{0}, time to wake up 🌞" to "Every morning is a fresh start. Be kind to yourself today, you deserve it.",
        "Happy morning, {0} 💛" to "The birds are singing for you. Start slow, smile big and have a beautiful day.",
    ),
)

/**
 * Title and text of a sleep reminder in the language [code], greeting [fullName] by first name
 * ("friend" when unknown). [variant] picks the message; by default it changes every day.
 */
fun sleepReminderText(
    key: String,
    code: String,
    fullName: String = "",
    variant: Int = LocalDate.now().dayOfYear,
): Pair<String, String> {
    val messages = SLEEP_MESSAGES[key] ?: SLEEP_MESSAGES.getValue("bedtime")
    val (title, text) = messages[Math.floorMod(variant, messages.size)]
    val name = fullName.trim().substringBefore(' ').ifEmpty { tIn(code, "friend") }
    return tIn(code, title, name) to tIn(code, text)
}
