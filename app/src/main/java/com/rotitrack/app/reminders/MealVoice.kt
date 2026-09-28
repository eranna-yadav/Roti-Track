package com.rotitrack.app.reminders

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.rotitrack.app.i18n.I18n
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Reads meal reminders out loud with the phone's text-to-speech voice. */
object MealVoice {
    private val EMOJI = Regex("[\\p{So}\\p{Cs}\\p{Sk}\\u200D\\uFE0F]")

    /** "Time to log your lunch 🍛" + "Tap to add…" → one sentence to speak, without emoji. */
    fun sentence(title: String, text: String): String =
        "${title.replace(EMOJI, "").trim()}. ${text.replace(EMOJI, "").trim()}".replace(Regex("\\s+"), " ")

    /** Stays quiet when the phone is on silent or vibrate, or Do Not Disturb is on. */
    fun allowed(context: Context): Boolean {
        val audio = context.getSystemService(AudioManager::class.java) ?: return false
        if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL) return false
        val filter = context.getSystemService(NotificationManager::class.java)?.currentInterruptionFilter
        return filter == null || filter <= NotificationManager.INTERRUPTION_FILTER_ALL
    }

    /**
     * Speaks [text] in the app's language. If the phone has no voice for it, speaks [english]
     * with an English voice instead. [onDone] runs once speaking ends or fails.
     */
    fun speak(context: Context, text: String, english: String, onDone: () -> Unit = {}) {
        var tts: TextToSpeech? = null
        var finished = false
        fun finish() {
            if (finished) return
            finished = true
            tts?.shutdown()
            onDone()
        }
        tts = TextToSpeech(context.applicationContext) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) return@TextToSpeech finish()
            val lang = I18n.lang
            val wanted = Locale(lang, "IN")
            val say = if (engine.isLanguageAvailable(wanted) >= TextToSpeech.LANG_AVAILABLE) {
                engine.setLanguage(wanted)
                text
            } else {
                val en = Locale("en", "IN")
                engine.setLanguage(if (engine.isLanguageAvailable(en) >= TextToSpeech.LANG_AVAILABLE) en else Locale.ENGLISH)
                english
            }
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) = finish()
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish()
                override fun onError(utteranceId: String?, errorCode: Int) = finish()
            })
            if (engine.speak(say, TextToSpeech.QUEUE_FLUSH, null, "meal-reminder") != TextToSpeech.SUCCESS) finish()
        }
    }

    /** For background work: speaks and waits until done, at most [timeoutSeconds]. */
    fun speakAndWait(context: Context, text: String, english: String, timeoutSeconds: Long = 30) {
        val latch = CountDownLatch(1)
        speak(context, text, english) { latch.countDown() }
        latch.await(timeoutSeconds, TimeUnit.SECONDS)
    }
}
