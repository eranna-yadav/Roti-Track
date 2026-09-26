package com.rotitrack.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.rotitrack.app.account.DemoBilling
import com.rotitrack.app.account.LocalAuth
import com.rotitrack.app.account.LocalDirectory
import com.rotitrack.app.account.Services
import com.rotitrack.app.cloud.FirebaseAuthService
import com.rotitrack.app.cloud.FirestoreDirectory
import com.rotitrack.app.cloud.PlayBilling
import com.rotitrack.app.reminders.Reminders
import com.rotitrack.app.store.AndroidStorage
import com.rotitrack.app.store.AppStore

/**
 * Builds the services once per process. With app/google-services.json present
 * accounts live in Firebase; without it they stay on this phone. Debug builds
 * use demo billing, release builds use Google Play Billing.
 */
class RotiTrackApp : Application() {
    lateinit var services: Services
        private set

    /** The resumed activity, needed to launch the Play purchase sheet. */
    var currentActivity: Activity? = null

    private val stores = mutableMapOf<String, AppStore>()
    private val prefs by lazy { getSharedPreferences("session", Context.MODE_PRIVATE) }

    val usesFirebase: Boolean get() = FirebaseApp.getApps(this).isNotEmpty()

    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)

        val (auth, directory) = if (usesFirebase) {
            val dir = FirestoreDirectory()
            FirebaseAuthService(dir) to dir
        } else {
            val dir = LocalDirectory(AndroidStorage(this, "directory.json"))
            LocalAuth(AndroidStorage(this, "accounts.json"), dir) to dir
        }
        val debuggable = applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
        val billing = if (debuggable) {
            DemoBilling(AndroidStorage(this, "demo-billing.json")) { auth.account?.uid }
        } else {
            PlayBilling(this, { currentActivity }, { auth.account?.uid }).also { it.connect() }
        }
        services = Services(auth, directory, billing, ::storeFor)
    }

    fun storeFor(uid: String): AppStore = stores.getOrPut(uid) { AppStore(AndroidStorage(this, "state-$uid.json")) }

    /** Remembered so the reminder worker knows whose log to read after a restart. */
    var activeUid: String?
        get() = prefs.getString("uid", null)
        set(value) = prefs.edit().putString("uid", value).apply()

    val activeStore: AppStore? get() = activeUid?.let(::storeFor)
}

val Context.rotiTrack: RotiTrackApp get() = applicationContext as RotiTrackApp
