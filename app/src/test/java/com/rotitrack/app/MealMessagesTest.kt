package com.rotitrack.app

import com.rotitrack.app.reminders.MEAL_MESSAGES
import com.rotitrack.app.reminders.mealReminderText
import org.junit.Assert.assertTrue
import org.junit.Test

class MealMessagesTest {
    /** The word for each meal, per language; End of Day reminders talk about dinner. */
    private val mealWords = mapOf(
        "en" to listOf("breakfast", "lunch", "snack", "dinner"),
        "hi" to listOf("नाश्त", "लंच", "स्नैक", "डिनर"),
        "kn" to listOf("ಉಪಾಹಾರ", "ಮಧ್ಯಾಹ್ನದ ಊಟ", "ತಿಂಡಿ", "ರಾತ್ರಿ ಊಟ"),
        "te" to listOf("అల్పాహార", "మధ్యాహ్న భోజన", "స్నాక్", "రాత్రి భోజన"),
        "ta" to listOf("காலை உணவ", "மதிய உணவ", "சிற்றுண்டி", "இரவு உணவ"),
        "mr" to listOf("नाश्त", "दुपारच्या जेवण", "स्नॅक", "रात्रीच"),
        "bn" to listOf("প্রাতরাশ", "দুপুরের খাবার", "জলখাবার", "রাতের খাবার"),
        "gu" to listOf("નાસ્ત", "બપોરના ભોજન", "નાસ્ત", "રાત્રિભોજન"),
    )

    @Test fun everyReminderSaysTheNameAndTheMealInEveryLanguage() {
        val meal = mapOf("breakfast" to 0, "lunch" to 1, "snack" to 2, "dinner" to 3, "end_of_day" to 3)
        for ((code, words) in mealWords) {
            for ((key, messages) in MEAL_MESSAGES) {
                for (variant in messages.indices) {
                    val (title, text) = mealReminderText(key, code, "Krish Kumar", variant)
                    assertTrue("$code/$key/$variant has no name: $title", title.contains("Krish") && !title.contains("Kumar"))
                    val word = words[meal.getValue(key)]
                    assertTrue("$code/$key/$variant has no \"$word\": $title", title.lowercase().contains(word))
                    assertTrue("$code/$key/$variant is untranslated", code == "en" || text != messages[variant].second)
                }
            }
        }
    }
}
