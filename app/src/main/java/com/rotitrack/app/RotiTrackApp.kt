package com.rotitrack.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.rotitrack.app.account.DemoBilling
import com.rotitrack.app.account.LocalAuth
import com.rotitrack.app.account.LocalDirectory
import com.rotitrack.app.account.NoRazorpay
import com.rotitrack.app.account.Services
import com.rotitrack.app.cloud.FirebaseAuthService
import com.rotitrack.app.cloud.FirestoreDirectory
import com.rotitrack.app.cloud.FirebaseRazorpay
import com.rotitrack.app.cloud.PlayBilling
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import com.rotitrack.app.i18n.I18n
import com.rotitrack.app.reminders.MealReminders
import com.rotitrack.app.reminders.Reminders
import com.rotitrack.app.store.AndroidStorage
import com.rotitrack.app.store.AppStore

/**
 * Builds the services once per process. With app/google-services.json present
 * accounts live in Firebase and Razorpay is available; without it everything
 * stays on this phone. Debug builds use demo Play billing, release builds the real one.
 */
class RotiTrackApp : Application() {
    lateinit var services: Services
        private set

    /** The resumed activity, needed to launch the Play purchase sheet and Razorpay Checkout. */
    var currentActivity: Activity? = null

    /** Receives Razorpay Checkout results from the activity. Null without Firebase. */
    var razorpay: FirebaseRazorpay? = null
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val stores = mutableMapOf<String, AppStore>()
    private val prefs by lazy { getSharedPreferences("session", Context.MODE_PRIVATE) }

    val usesFirebase: Boolean get() = FirebaseApp.getApps(this).isNotEmpty()

    override fun onCreate() {
        super.onCreate()
        // Notifications and reminder workers speak the signed-in user's language too.
        I18n.lang = I18n.resolve(activeUid?.let { storeFor(it).prefs.language }.orEmpty())
        Reminders.createChannel(this)
        MealReminders.createChannel(this)

        val (auth, directory) = if (usesFirebase) {
            val dir = FirestoreDirectory()
            FirebaseAuthService(dir) to dir
        } else {
            val dir = LocalDirectory(AndroidStorage(this, "directory.json"))
            LocalAuth(AndroidStorage(this, "accounts.json"), dir) to dir
        }
        val razorpay = if (usesFirebase) FirebaseRazorpay { currentActivity } else NoRazorpay()
        this.razorpay = razorpay as? FirebaseRazorpay

        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val billing = if (debuggable) {
            DemoBilling(AndroidStorage(this, "demo-billing.json")) { auth.account?.uid }
        } else {
            PlayBilling(
                this, { currentActivity }, { auth.account?.uid },
                offersAlternative = resources.getBoolean(R.bool.user_choice_billing) && usesFirebase,
                onAlternativeChosen = { plan, token, referralCode ->
                    // The user picked Razorpay on Google Play's choice screen.
                    auth.account?.let { account ->
                        scope.launch { runCatching { razorpay.subscribe(plan, account, token, referralCode) } }
                    }
                },
            ).also { it.connect() }
        }
        services = Services(auth, directory, billing, razorpay, ::storeFor)
    }

    fun storeFor(uid: String): AppStore = stores.getOrPut(uid) { AppStore(AndroidStorage(this, "state-$uid.json")) }

    /** Remembered so the reminder worker knows whose log to read after a restart. */
    var activeUid: String?
        get() = prefs.getString("uid", null)
        set(value) = prefs.edit().putString("uid", value).apply()

    val activeStore: AppStore? get() = activeUid?.let(::storeFor)
}

val Context.rotiTrack: RotiTrackApp get() = applicationContext as RotiTrackApp
