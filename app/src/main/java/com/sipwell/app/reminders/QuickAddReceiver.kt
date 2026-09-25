package com.sipwell.app.reminders

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.sipwell.app.appStore

/** The "+ 250 ml" button on a reminder: logs the usual glass without opening the app. */
class QuickAddReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = context.appStore
        store.addWater(store.profile.cupMl)
        NotificationManagerCompat.from(context).cancel(Reminders.NOTIFICATION_ID)
    }
}
