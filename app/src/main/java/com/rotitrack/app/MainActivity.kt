package com.rotitrack.app

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
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.rotitrack.app.data.Profile
import com.rotitrack.app.reminders.Reminders
import com.rotitrack.app.ui.App
import com.rotitrack.app.ui.Navigator
import com.rotitrack.app.ui.Platform

class MainActivity : ComponentActivity(), Platform, PaymentResultWithDataListener {

    private var pendingResult: ((Boolean) -> Unit)? = null

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingResult?.invoke(granted)
        pendingResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (rotiTrack.razorpay != null) Checkout.preload(applicationContext)
        // The app is always light, so keep dark system-bar icons even when the phone is in dark mode.
        val bars = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
        setContent {
            val nav = remember { Navigator() }
            BackHandler(enabled = nav.canPop) { nav.pop() }
            App(rotiTrack.services, this, nav)
        }
    }

    override fun onResume() {
        super.onResume()
        rotiTrack.currentActivity = this
        rotiTrack.services.billing.restore()
    }

    override fun onPause() {
        if (rotiTrack.currentActivity === this) rotiTrack.currentActivity = null
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
        rotiTrack.activeUid = uid
        if (uid == null) {
            Reminders.cancel(this)
        } else {
            rotiTrack.services.billing.restore()
            Reminders.schedule(this, rotiTrack.storeFor(uid).profile)
        }
    }

    // Razorpay Checkout reports back to the activity that opened it.
    override fun onPaymentSuccess(paymentId: String?, data: PaymentData?) {
        rotiTrack.razorpay?.onPaymentSuccess(paymentId, data)
    }

    override fun onPaymentError(code: Int, response: String?, data: PaymentData?) {
        rotiTrack.razorpay?.onPaymentError(code, response)
    }

    override fun openUrl(url: String) {
        runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
    }
}
