package com.rotitrack.app.domain

import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

/** Days are keyed as ISO dates ("2026-09-25"), which also sort correctly as strings. */
object Days {
    fun today(): String = LocalDate.now().toString()
    fun parse(key: String): LocalDate = LocalDate.parse(key)
    fun shift(key: String, days: Long): String = parse(key).plusDays(days).toString()

    fun weekdayShort(key: String): String =
        parse(key).dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())

    fun label(key: String, today: String = today()): String = when (key) {
        today -> "Today"
        shift(today, -1) -> "Yesterday"
        else -> parse(key).let { "${it.dayOfMonth} ${it.month.getDisplayName(TextStyle.SHORT, Locale.getDefault())}" }
    }

    /** The [count] day keys ending with [end], oldest first. */
    fun lastDays(end: String, count: Int): List<String> = (count - 1 downTo 0).map { shift(end, -it.toLong()) }
}
