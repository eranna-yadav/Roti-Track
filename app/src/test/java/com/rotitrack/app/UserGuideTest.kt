package com.rotitrack.app

import com.rotitrack.app.data.USER_GUIDE
import com.rotitrack.app.data.guidePage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserGuideTest {
    @Test fun theGuideCoversEveryTabWithCompleteFeatures() {
        assertEquals(listOf("home", "water", "food", "plan", "profile"), USER_GUIDE.map { it.id })
        for (page in USER_GUIDE) {
            assertTrue(page.id, page.features.size >= 4)
            assertEquals(page, guidePage(page.id))
            for (f in page.features) {
                assertTrue(f.title, f.what.isNotBlank() && f.benefit.isNotBlank() && f.how.isNotEmpty())
                assertTrue(f.title, f.how.all { it.isNotBlank() })
            }
        }
    }
}
