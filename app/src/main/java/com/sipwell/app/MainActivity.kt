package com.sipwell.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import com.sipwell.app.data.Profile
import com.sipwell.app.reminders.Reminders
import com.sipwell.app.ui.App
import com.sipwell.app.ui.Navigator
import com.sipwell.app.ui.Platform

class MainActivity : ComponentActivity(), Platform {

    private var pendingResult: ((Boolean) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingResult?.invoke(granted)
        pendingResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app is always light, so keep dark system-bar icons even when the phone is in dark mode.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            val nav = remember { Navigator() }
            BackHandler(enabled = nav.canPop) { nav.pop() }
            App(sipwell.services, this, nav)
        }
    }

    override fun onResume() {
        super.onResume()
        sipwell.currentActivity = this
        sipwell.services.billing.restore()
    }

    override fun onPause() {
        if (sipwell.currentActivity === this) sipwell.currentActivity = null
        super.onPause()
    }

    override fun requestNotifications(onResult: (Boolean) -> Unit) {
        val needsAsk = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        if (!needsAsk) {
            onResult(true)
            return
        }
        pendingResult = onResult
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    override fun scheduleReminders(profile: Profile) = Reminders.schedule(this, profile)

    override fun sessionChanged(uid: String?) {
        sipwell.activeUid = uid
        if (uid == null) {
            Reminders.cancel(this)
        } else {
            sipwell.services.billing.restore()
            Reminders.schedule(this, sipwell.storeFor(uid).profile)
        }
    }

    override fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}
