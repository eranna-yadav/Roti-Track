package com.rotitrack.app.cloud

import com.rotitrack.app.i18n.t
import android.app.Activity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import com.razorpay.Checkout
import com.razorpay.PaymentData
import com.rotitrack.app.account.Account
import com.rotitrack.app.account.AuthException
import com.rotitrack.app.account.Plan
import com.rotitrack.app.account.RazorpayGateway
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Razorpay subscriptions through Checkout, backed by the Cloud Functions in
 * /functions. The activity forwards Checkout's callbacks to [onPaymentSuccess]
 * and [onPaymentError].
 */
class FirebaseRazorpay(private val activity: () -> Activity?) : RazorpayGateway {
    private val functions = FirebaseFunctions.getInstance(REGION)
    private var pending: CompletableDeferred<Result<PaymentData>>? = null

    override val unavailableReason: String? = null
    override var busy by mutableStateOf(false)
        private set
    override var message by mutableStateOf<String?>(null)
        private set
    override var version by mutableIntStateOf(0)
        private set

    override suspend fun subscribe(plan: Plan, account: Account, externalTransactionToken: String?) = guarded {
        // 1. The server creates the subscription with the secret key.
        val created = call(
            "createRazorpaySubscription",
            mapOf("plan" to plan.productId, "externalTransactionToken" to externalTransactionToken),
        )
        val subscriptionId = created["subscriptionId"] as String
        val act = activity() ?: throw AuthException(t("Open Roti Track and try again."))

        // 2. Razorpay Checkout collects the payment (UPI, cards, netbanking, wallets).
        val result = CompletableDeferred<Result<PaymentData>>().also { pending = it }
        val options = JSONObject()
            .put("name", "Roti Track")
            .put(
                "description",
                if (created["referralPrice"] == true && plan.referralPrice != null) t("Pro · {0} ({1} for the first year)", plan.label, plan.referralPrice)
                else t("Pro · {0} ({1}/{2})", plan.label, plan.fallbackPrice, plan.period),
            )
            .put("subscription_id", subscriptionId)
            .put("theme", JSONObject().put("color", "#1B4FF0"))
            .put("prefill", JSONObject().put("email", account.email).put("name", account.name))
            .put("notes", JSONObject().put("uid", account.uid))
        Checkout().apply { setKeyID(created["keyId"] as String) }.open(act, options)
        val data = result.await().getOrThrow()

        // 3. The server checks the signature and switches Pro on.
        call(
            "verifyRazorpayPayment",
            mapOf(
                "paymentId" to data.paymentId,
                "subscriptionId" to (data.data?.optString("razorpay_subscription_id")?.takeIf { it.isNotEmpty() } ?: subscriptionId),
                "signature" to data.signature,
            ),
        )
        message = t("Payment successful. Welcome to Roti Track Pro!")
    }

    override suspend fun cancel() = guarded {
        call("cancelRazorpaySubscription", emptyMap())
        message = t("Auto-renew is off. You keep Pro until the end of this period.")
    }

    fun onPaymentSuccess(paymentId: String?, data: PaymentData?) {
        pending?.complete(if (data != null && paymentId != null) Result.success(data) else Result.failure(AuthException(t("Payment incomplete"))))
        pending = null
    }

    fun onPaymentError(code: Int, response: String?) {
        val reason = if (code == Checkout.PAYMENT_CANCELED) t("Payment cancelled.") else t("Payment failed. No money was taken; please try again.")
        pending?.complete(Result.failure(AuthException(reason)))
        pending = null
    }

    /** Shared busy/message handling; bumps [version] so the app reloads the account on success. */
    private suspend fun guarded(block: suspend () -> Unit) {
        busy = true
        message = null
        try {
            block()
            version++
        } catch (e: FirebaseFunctionsException) {
            message = e.message ?: t("Couldn't reach the payment server.")
            throw AuthException(message!!)
        } catch (e: Exception) {
            message = e.message ?: t("Something went wrong.")
            throw e
        } finally {
            busy = false
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun call(name: String, data: Map<String, Any?>): Map<String, Any?> =
        (functions.getHttpsCallable(name).call(data).await().getData() as? Map<String, Any?>).orEmpty()

    companion object {
        /** Must match REGION in functions/index.js. */
        const val REGION = "asia-south1"
    }
}
