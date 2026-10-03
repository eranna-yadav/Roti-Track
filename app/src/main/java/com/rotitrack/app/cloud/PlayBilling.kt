package com.rotitrack.app.cloud

import com.rotitrack.app.i18n.t
import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.rotitrack.app.account.Billing
import com.rotitrack.app.account.Plan

/**
 * Google Play Billing for the two Pro subscriptions. Purchases are tagged with
 * the app account's uid, so Pro follows the Roti Track account, not just the phone.
 *
 * Note: entitlement is checked on the device. For stronger protection, verify
 * purchase tokens on a server with the Google Play Developer API.
 */
private const val REFERRAL_OFFER_TAG = "referral"

class PlayBilling(
    context: Context,
    private val activity: () -> Activity?,
    private val uid: () -> String?,
    /**
     * Google Play user choice billing (India): when on, Play's purchase flow first shows a
     * choice screen, and picking Razorpay calls [onAlternativeChosen] with Google's
     * external transaction token. Only turn on once enrolled in Play Console.
     */
    override val offersAlternative: Boolean,
    /** Plan, Google's external transaction token, and the referral code entered before paying. */
    private val onAlternativeChosen: (Plan, String, String?) -> Unit,
) : Billing, PurchasesUpdatedListener {

    private val client = BillingClient.newBuilder(context)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .apply {
            if (offersAlternative) {
                enableUserChoiceBilling { details ->
                    val plan = details.products.firstNotNullOfOrNull { Plan.byProductId(it.id) }
                    if (plan != null) onAlternativeChosen(plan, details.externalTransactionToken, pendingReferralCode)
                }
            }
        }
        .build()

    private var details by mutableStateOf<Map<Plan, ProductDetails>>(emptyMap())
    private var purchases by mutableStateOf<List<Purchase>>(emptyList())

    override var unavailableReason: String? by mutableStateOf(t("Connecting to Google Play…"))
        private set

    override val activePlan: Plan?
        get() {
            val me = uid() ?: return null
            return purchases
                .filter { it.purchaseState == Purchase.PurchaseState.PURCHASED }
                .filter { it.accountIdentifiers?.obfuscatedAccountId.let { id -> id == null || id == me } }
                .flatMap { it.products }
                .mapNotNull { Plan.byProductId(it) }
                .maxByOrNull { it.rupees }
        }

    fun connect() {
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    unavailableReason = null
                    loadProducts()
                    restore()
                } else {
                    unavailableReason = t("Google Play Billing isn't available. Install Roti Track from Google Play to subscribe.")
                }
            }

            override fun onBillingServiceDisconnected() {
                unavailableReason = t("Lost connection to Google Play. Reopen this screen to try again.")
            }
        })
    }

    private fun loadProducts() {
        val params = QueryProductDetailsParams.newBuilder().setProductList(
            Plan.entries.map {
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(it.productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }
        ).build()
        client.queryProductDetailsAsync(params) { result, found ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                details = found.productDetailsList.mapNotNull { pd -> Plan.byProductId(pd.productId)?.let { it to pd } }.toMap()
                if (details.isEmpty()) unavailableReason = t("Pro plans aren't available yet. Please try again later.")
            }
        }
    }

    /** The base plan (no intro offer) of a subscription. */
    private fun baseOffer(plan: Plan) = details[plan]?.subscriptionOfferDetails?.let { offers ->
        offers.firstOrNull { it.offerId == null } ?: offers.firstOrNull()
    }

    override fun price(plan: Plan): String =
        baseOffer(plan)?.pricingPhases?.pricingPhaseList?.lastOrNull()?.formattedPrice ?: plan.fallbackPrice

    /** The first-year discount for referred users, set up in Play Console as an offer tagged "referral". */
    private fun referralOffer(plan: Plan) =
        details[plan]?.subscriptionOfferDetails?.firstOrNull { REFERRAL_OFFER_TAG in it.offerTags }

    override fun referralPrice(plan: Plan): String? =
        referralOffer(plan)?.pricingPhases?.pricingPhaseList?.firstOrNull()?.formattedPrice

    /** Kept for Play's choice screen, in case the user picks Razorpay there. */
    private var pendingReferralCode: String? = null

    override fun purchase(plan: Plan, referralCode: String?) {
        val act = activity() ?: return
        val pd = details[plan] ?: return
        pendingReferralCode = referralCode
        val offer = (if (referralCode != null) referralOffer(plan) else null) ?: baseOffer(plan) ?: return
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pd).setOfferToken(offer.offerToken).build())
            )
            .apply { uid()?.let { setObfuscatedAccountId(it) } }
            .build()
        client.launchBillingFlow(act, params)
    }

    override fun restore() {
        if (!client.isReady) return
        client.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder().setProductType(BillingClient.ProductType.SUBS).build()
        ) { result, list -> if (result.responseCode == BillingClient.BillingResponseCode.OK) handle(list) }
    }

    override fun onPurchasesUpdated(result: BillingResult, list: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && list != null) handle(list + purchases.filter { p -> list.none { it.orderId == p.orderId } })
    }

    private fun handle(list: List<Purchase>) {
        purchases = list
        // Unacknowledged subscriptions are refunded by Google after three days.
        list.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }.forEach { p ->
            client.acknowledgePurchase(AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()) {}
        }
    }
}
