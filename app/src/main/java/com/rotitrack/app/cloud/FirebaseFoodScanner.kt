package com.rotitrack.app.cloud

import android.util.Base64
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.rotitrack.app.account.AuthException
import com.rotitrack.app.account.FoodScanner
import com.rotitrack.app.account.ScanResult
import com.rotitrack.app.account.scanResultFrom
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.i18n.t
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

/** Sends the photo to the scanFood function in /functions, which asks Claude what's on the plate. */
class FirebaseFoodScanner : FoodScanner {
    private val functions = FirebaseFunctions.getInstance(FirebaseRazorpay.REGION)

    override val unavailableReason: String? = null

    override suspend fun scan(jpeg: ByteArray, meal: MealSlot): ScanResult {
        val data = mapOf(
            "image" to Base64.encodeToString(jpeg, Base64.NO_WRAP),
            "meal" to meal.name.lowercase(),
        )
        try {
            val result = functions.getHttpsCallable("scanFood").withTimeout(120, TimeUnit.SECONDS).call(data).await()
            return scanResultFrom(result.getData() as? Map<*, *>)
        } catch (e: FirebaseFunctionsException) {
            // The server's messages are written for users; a timeout gets its own.
            val message = if (e.code == FirebaseFunctionsException.Code.DEADLINE_EXCEEDED) {
                t("The server is taking too long. Check your internet connection and try again.")
            } else {
                // Messages from functions/index.js are translation keys too.
                e.message?.takeIf { it.isNotBlank() }?.let { t(it) }
            }
            throw AuthException(message ?: t("Something went wrong. Try again."))
        }
    }
}
