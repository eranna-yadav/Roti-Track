package com.rotitrack.app.domain

/**
 * When to show Google Play's "Rate Roti Track" popup: only at a happy moment, after the user
 * has used the app for a few days in a row, at most 3 times and never twice within 60 days.
 * Google may still choose not to show it (it has its own limits).
 */
object ReviewPrompt {
    const val MIN_STREAK = 3
    const val MAX_ASKS = 3
    const val GAP_MS = 60L * 24 * 3_600_000

    fun due(now: Long, streak: Int, askedAt: Long, asks: Int): Boolean =
        streak >= MIN_STREAK && asks < MAX_ASKS && (askedAt == 0L || now - askedAt >= GAP_MS)
}
