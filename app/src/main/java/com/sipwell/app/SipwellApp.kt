package com.sipwell.app

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.firebase.FirebaseApp
import com.sipwell.app.account.DemoBilling
import com.sipwell.app.account.LocalAuth
import com.sipwell.app.account.LocalDirectory
import com.sipwell.app.account.Services
import com.sipwell.app.cloud.FirebaseAuthService
import com.sipwell.app.cloud.FirestoreDirectory
import com.sipwell.app.cloud.PlayBilling
import com.sipwell.app.reminders.Reminders
import com.sipwell.app.store.AndroidStorage
import com.sipwell.app.store.AppStore
import java.io.File

/**
 * Builds the services once per process. With app/google-services.json present
 * accounts live in Firebase; without it they stay on this phone. Debug builds
 * use demo billing, release builds use Google Play Billing.
 */
class SipwellApp : Application() {
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

    fun storeFor(uid: String): AppStore = stores.getOrPut(uid) {
        val name = "state-$uid.json"
        // The single-user 1.0 app kept one file; hand it to the first account that signs in.
        val legacy = File(filesDir, "sipwell.json")
        if (legacy.exists() && !File(filesDir, name).exists()) legacy.renameTo(File(filesDir, name))
        AppStore(AndroidStorage(this, name))
    }

    /** Remembered so the reminder worker knows whose log to read after a restart. */
    var activeUid: String?
        get() = prefs.getString("uid", null)
        set(value) = prefs.edit().putString("uid", value).apply()

    val activeStore: AppStore? get() = activeUid?.let(::storeFor)
}

val Context.sipwell: SipwellApp get() = applicationContext as SipwellApp
