package com.sipwell.app

import android.app.Application
import com.sipwell.app.reminders.Reminders
import com.sipwell.app.store.AndroidStorage
import com.sipwell.app.store.AppStore

/** Holds the one [AppStore] shared by the UI, the reminder worker and the quick-add action. */
class SipwellApp : Application() {
    lateinit var store: AppStore
        private set

    override fun onCreate() {
        super.onCreate()
        store = AppStore(AndroidStorage(this))
        Reminders.createChannel(this)
    }
}

val android.content.Context.appStore: AppStore get() = (applicationContext as SipwellApp).store
