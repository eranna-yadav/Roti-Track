package com.rotitrack.app.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * The app's language. Text is looked up by its English wording, so anything without a
 * translation simply shows in English. [lang] is Compose state: changing it redraws the screens.
 */
object I18n {
    val SUPPORTED = listOf("en", "hi", "te", "ta", "kn", "mr", "bn", "gu")

    var lang by mutableStateOf(resolve(""))

    private val tables: Map<String, Map<String, String>> by lazy {
        mapOf("hi" to HI, "te" to TE, "ta" to TA, "kn" to KN, "mr" to MR, "bn" to BN, "gu" to GU)
    }

    /** The saved choice, or the phone's language when none was made and we support it, else English. */
    fun resolve(saved: String): String = when {
        saved in SUPPORTED -> saved
        else -> Locale.getDefault().language.takeIf { it in SUPPORTED } ?: "en"
    }

    fun lookup(key: String): String? = if (lang == "en") null else tables[lang]?.get(key)

    /** For dates and month names in the chosen language. */
    val locale: Locale get() = if (lang == "en") Locale.ENGLISH else Locale(lang, "IN")

    fun table(code: String): Map<String, String> = tables[code].orEmpty()
}

/**
 * Translates [key], the English text. Placeholders {0}, {1}… are filled with [args] in order,
 * so translations can move them around the sentence.
 */
fun t(key: String, vararg args: Any?): String = fill(I18n.lookup(key) ?: key, args)

/** Like [t], but in the language [code] whatever the app is showing, e.g. "en" for a fallback. */
fun tIn(code: String, key: String, vararg args: Any?): String = fill(I18n.table(code)[key] ?: key, args)

private fun fill(template: String, args: Array<out Any?>): String {
    if (args.isEmpty()) return template
    val sb = StringBuilder(template.length + 16)
    var i = 0
    while (i < template.length) {
        val c = template[i]
        if (c == '{') {
            val end = template.indexOf('}', i)
            val n = if (end > i) template.substring(i + 1, end).toIntOrNull() else null
            if (n != null && n in args.indices) {
                sb.append(args[n].toString())
                i = end + 1
                continue
            }
        }
        sb.append(c)
        i++
    }
    return sb.toString()
}
