package com.rotitrack.app.reminders

import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import com.rotitrack.app.i18n.I18n
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Reads meal reminders out loud with the phone's text-to-speech voice. */
object MealVoice {
    private val EMOJI = Regex("[\\p{So}\\p{Cs}\\p{Sk}\\u200D\\uFE0F]")

    /** A friendly, unhurried pace and a slightly brighter pitch sound warmer than the defaults. */
    private const val RATE = 0.9f
    private const val PITCH = 1.08f
    /** A short breath between sentences, as a person would pause. */
    private const val PAUSE_MS = 350L
    private const val LAST = "meal-reminder-end"

    /** "Hey Krish! Lunch time 🍛" + "Tap to add…" → one passage to speak, without emoji. */
    fun sentence(title: String, text: String): String {
        val head = title.replace(EMOJI, "").trim()
        val end = if (head.isNotEmpty() && head.last() in ".!?।") "" else "."
        return "$head$end ${text.replace(EMOJI, "").trim()}".replace(Regex("\\s+"), " ").trim()
    }

    /** Sentences to speak one by one, so there's a natural pause between them. */
    private fun phrases(text: String): List<String> =
        text.split(Regex("(?<=[.!?।])\\s+|\\s+—\\s+")).map { it.trim() }.filter { it.isNotEmpty() }

    /** The most natural installed voice for [locale], if the engine lists any. */
    private fun bestVoice(engine: TextToSpeech, locale: Locale): Voice? =
        runCatching { engine.voices }.getOrNull().orEmpty()
            .filter { v ->
                v.locale.language == locale.language &&
                    !v.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in v.features
            }
            .maxWithOrNull(compareBy<Voice>({ it.locale.country == locale.country }, { it.quality }, { -it.latency }))

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
                bestVoice(engine, wanted)?.let { engine.setVoice(it) }
                text
            } else {
                val en = Locale("en", "IN")
                val locale = if (engine.isLanguageAvailable(en) >= TextToSpeech.LANG_AVAILABLE) en else Locale.ENGLISH
                engine.setLanguage(locale)
                bestVoice(engine, locale)?.let { engine.setVoice(it) }
                english
            }
            engine.setSpeechRate(RATE)
            engine.setPitch(PITCH)
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) {
                    if (utteranceId == LAST) finish()
                }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) = finish()
                override fun onError(utteranceId: String?, errorCode: Int) = finish()
            })
            val parts = phrases(say).ifEmpty { listOf(say) }
            var ok = true
            parts.forEachIndexed { i, part ->
                if (i > 0) engine.playSilentUtterance(PAUSE_MS, TextToSpeech.QUEUE_ADD, "meal-reminder-pause-$i")
                val id = if (i == parts.lastIndex) LAST else "meal-reminder-$i"
                val mode = if (i == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                if (engine.speak(part, mode, null, id) != TextToSpeech.SUCCESS) ok = false
            }
            if (!ok) finish()
        }
    }

    /** For background work: speaks and waits until done, at most [timeoutSeconds]. */
    fun speakAndWait(context: Context, text: String, english: String, timeoutSeconds: Long = 30) {
        val latch = CountDownLatch(1)
        speak(context, text, english) { latch.countDown() }
        latch.await(timeoutSeconds, TimeUnit.SECONDS)
    }
}
