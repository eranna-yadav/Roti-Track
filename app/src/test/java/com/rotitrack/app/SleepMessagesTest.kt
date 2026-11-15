package com.rotitrack.app

import com.rotitrack.app.reminders.SLEEP_MESSAGES
import com.rotitrack.app.reminders.sleepReminderText
import org.junit.Assert.assertTrue
import org.junit.Test

class SleepMessagesTest {
    @Test fun everySleepReminderSaysTheNameAndIsTranslated() {
        for (code in listOf("en", "hi", "mr", "gu", "bn", "ta", "te", "kn")) {
            for ((key, messages) in SLEEP_MESSAGES) {
                for (variant in messages.indices) {
                    val (title, text) = sleepReminderText(key, code, "Krish Kumar", variant)
                    assertTrue("$code/$key/$variant has no name: $title", title.contains("Krish") && !title.contains("Kumar"))
                    assertTrue("$code/$key/$variant is untranslated", code == "en" || (title != messages[variant].first.replace("{0}", "Krish") && text != messages[variant].second))
                }
            }
        }
    }

    @Test fun noNameFallsBackToFriend() {
        assertTrue(sleepReminderText("wake", "en", "", 0).first.contains("friend"))
    }
}
