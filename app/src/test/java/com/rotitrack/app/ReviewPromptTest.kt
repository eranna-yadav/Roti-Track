package com.rotitrack.app

import com.rotitrack.app.domain.ReviewPrompt
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPromptTest {
    private val day = 24 * 3_600_000L
    private val now = 1_000 * day

    @Test fun asksOnlyRegularUsersAndRarely() {
        assertFalse("new user", ReviewPrompt.due(now, streak = 1, askedAt = 0, asks = 0))
        assertTrue(ReviewPrompt.due(now, streak = 3, askedAt = 0, asks = 0))
        assertFalse("asked a month ago", ReviewPrompt.due(now, streak = 10, askedAt = now - 30 * day, asks = 1))
        assertTrue(ReviewPrompt.due(now, streak = 10, askedAt = now - 61 * day, asks = 1))
        assertFalse("asked 3 times already", ReviewPrompt.due(now, streak = 10, askedAt = now - 365 * day, asks = 3))
    }
}
