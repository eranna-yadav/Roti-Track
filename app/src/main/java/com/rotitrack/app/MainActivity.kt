package com.rotitrack.app

import com.rotitrack.app.i18n.t
import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.razorpay.PaymentResultWithDataListener
import com.rotitrack.app.data.Prefs
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.WaterSound
import com.rotitrack.app.domain.Report
import com.rotitrack.app.i18n.I18n
import com.rotitrack.app.reminders.Alarms
import com.rotitrack.app.reminders.MealReminders
import com.rotitrack.app.reminders.MealVoice
import com.rotitrack.app.reminders.mealReminderText
import com.rotitrack.app.report.PdfReport
import com.rotitrack.app.reminders.Reminders
import com.rotitrack.app.reminders.WaterSounds
import com.rotitrack.app.ui.App
import com.rotitrack.app.ui.Navigator
import com.rotitrack.app.ui.Platform

class MainActivity : ComponentActivity(), Platform, PaymentResultWithDataListener {

    private var pendingResult: ((Boolean) -> Unit)? = null
    private var exactOk by mutableStateOf(true)

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        pendingResult?.invoke(granted)
        pendingResult = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (rotiTrack.razorpay != null) Checkout.preload(applicationContext)
        setDarkTheme(false)
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
        // Picks up a change made in system settings, and makes pending reminders exact once allowed.
        val allowed = Alarms.exactAllowed(this)
        if (allowed != exactOk) {
            exactOk = allowed
            Alarms.rescheduleAll(this)
        }
    }

    override val exactAlarmsAllowed: Boolean get() = exactOk

    override fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching {
                startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName")))
            }
        }
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

    override fun scheduleReminders(profile: Profile) {
        val prefs = rotiTrack.activeStore?.prefs ?: Prefs()
        Reminders.schedule(this, profile, prefs)
        MealReminders.schedule(this, prefs.mealReminders)
    }

    override fun sessionChanged(uid: String?) {
        rotiTrack.activeUid = uid
        if (uid == null) {
            Reminders.cancel(this)
            MealReminders.cancelAll(this)
        } else {
            rotiTrack.services.billing.restore()
            val store = rotiTrack.storeFor(uid)
            Reminders.schedule(this, store.profile, store.prefs)
            MealReminders.schedule(this, store.prefs.mealReminders)
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

    override fun share(text: String) {
        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
        startActivity(Intent.createChooser(send, t("Share Roti Track")))
    }

    override fun copyText(text: String) {
        getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Roti Track", text))
        Toast.makeText(this, t("Copied {0}", text), Toast.LENGTH_SHORT).show()
    }

    override fun notificationsEnabled(): Boolean = NotificationManagerCompat.from(this).areNotificationsEnabled()

    override fun openNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        runCatching { startActivity(intent) }
    }

    override fun openBatterySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
            .onFailure { openNotificationSettings() }
    }

    override fun playSound(sound: WaterSound, volume: Float) = WaterSounds.play(this, sound, volume)

    override fun stopSound() = WaterSounds.stop()

    override fun vibrate() = WaterSounds.vibrate(this)

    override fun previewMealVoice() {
        val name = rotiTrack.activeStore?.profile?.name.orEmpty()
        // A different meal and message on each tap, so all of them can be heard.
        val key = listOf("breakfast", "lunch", "snack", "dinner", "end_of_day").random()
        val variant = (0..2).random()
        val (title, text) = mealReminderText(key, I18n.lang, name, variant)
        val (enTitle, enText) = mealReminderText(key, "en", name, variant)
        MealVoice.speak(this, MealVoice.sentence(title, text), MealVoice.sentence(enTitle, enText))
    }

    override fun exportReport(report: Report) {
        val file = PdfReport.write(this, report)
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType("application/pdf")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_SUBJECT, "Roti Track summary report")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, t("Save or share your report")))
    }

    override fun setDarkTheme(dark: Boolean) {
        val bars = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = bars, navigationBarStyle = bars)
    }
}
