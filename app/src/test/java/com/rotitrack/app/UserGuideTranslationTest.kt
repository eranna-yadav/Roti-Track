package com.rotitrack.app

import com.rotitrack.app.data.USER_GUIDE
import com.rotitrack.app.i18n.I18n
import org.junit.Assert.assertTrue
import org.junit.Test

class UserGuideTranslationTest {
    @Test fun everyGuideSentenceIsTranslatedInEveryLanguage() {
        val texts = USER_GUIDE.flatMap { p ->
            listOf(p.title, p.intro) + p.features.flatMap { f -> listOf(f.title, f.what, f.benefit) + f.how }
        }.distinct()
        for (code in I18n.SUPPORTED - "en") {
            val table = I18n.table(code)
            val missing = texts.filter { table[it].isNullOrBlank() }
            assertTrue("$code is missing ${missing.size}: ${missing.take(3)}", missing.isEmpty())
        }
    }
}
