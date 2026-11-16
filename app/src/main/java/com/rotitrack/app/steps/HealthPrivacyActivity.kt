package com.rotitrack.app.steps

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle

/**
 * Health Connect opens this when the user asks why Roti Track reads their steps: it shows the
 * privacy policy in the browser.
 */
class HealthPrivacyActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://roti-track.web.app/privacy#health"))) }
        finish()
    }
}
