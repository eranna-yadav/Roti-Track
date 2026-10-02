package com.rotitrack.app.account

import com.rotitrack.app.i18n.t

import kotlinx.serialization.Serializable

data class Account(val uid: String, val name: String, val email: String, val isAdmin: Boolean)

/** Play Console product IDs. Create two auto-renewing subscriptions with these IDs. */
enum class Plan(
    val productId: String,
    private val labelEn: String,
    val fallbackPrice: String,
    private val periodEn: String,
    val rupees: Int,
    /** First-year price for users who signed up with a friend's referral code. */
    val referralRupees: Int? = null,
) {
    MONTHLY("rotitrack_pro_monthly", "Monthly", "₹359", "month", 359),
    YEARLY("rotitrack_pro_yearly", "Yearly", "₹1,099", "year", 1099, referralRupees = 990);

    val referralPrice: String? get() = referralRupees?.let { "₹$it" }

    val label: String get() = t(labelEn)
    val period: String get() = t(periodEn)

    companion object {
        fun byProductId(id: String?): Plan? = entries.firstOrNull { it.productId == id }
    }
}

/**
 * What the admin dashboard knows about a user. The app publishes the
 * activity fields; [compPro], [blocked] and [isAdmin] are only ever written by
 * an admin, and the razorpay* fields only by the payments server.
 */
@Serializable
data class UserSummary(
    val uid: String,
    val name: String = "",
    val email: String = "",
    val createdAt: Long = 0,
    val lastActive: Long = 0,
    /** Plan the user bought in the app ("rotitrack_pro_monthly"…), as last reported by the device. */
    val planId: String? = null,
    val compPro: Boolean = false,
    val blocked: Boolean = false,
    val isAdmin: Boolean = false,
    val calorieGoal: Int = 0,
    val waterGoalMl: Int = 0,
    val streak: Int = 0,
    val daysLogged: Int = 0,
    val diet: String = "",
    /** Set by the server after a verified Razorpay payment or webhook. */
    val razorpayPlanId: String? = null,
    val razorpayStatus: String? = null,
    val razorpayUntil: Long = 0,
    /** This user's own code to share, e.g. "RTK7P2QX". */
    val referralCode: String = "",
    /** The code this user signed up with, if any. Set once, at sign-up. */
    val referredByCode: String? = null,
    /** Server-maintained: ₹ earned and friends who went Pro with this user's code. */
    val referralEarnings: Int = 0,
    val referralCount: Int = 0,
    /** Server-maintained: this user's own Pro purchase has already paid out their referrer. */
    val referralCredited: Boolean = false,
    /** ₹ this user's payments have earned their referrer so far (0, 250 or 500). */
    val referralCreditedAmount: Int = 0,
    /** Where referral earnings should be paid. */
    val payoutUpi: String = "",
    /** Admin-maintained: ₹ of [referralEarnings] already paid out. */
    val referralPaid: Int = 0,
) {
    val referralDue: Int get() = (referralEarnings - referralPaid).coerceAtLeast(0)

    /** Razorpay plan that is paid up right now; stays until the cycle ends even if auto-renew is off. */
    val razorpayPlan: Plan? get() = if (razorpayUntil > System.currentTimeMillis()) Plan.byProductId(razorpayPlanId) else null
    val plan: Plan? get() = Plan.byProductId(planId) ?: razorpayPlan
    val paidVia: String? get() = when {
        Plan.byProductId(planId) != null -> "Google Play"
        razorpayPlan != null -> "Razorpay"
        else -> null
    }
    val isPro: Boolean get() = plan != null || compPro

    /**
     * Signed up with a friend's code and hasn't subscribed before, so the yearly plan's
     * first year is at [Plan.referralRupees]. The server checks this again before charging.
     */
    val referralPriceEligible: Boolean get() = !referredByCode.isNullOrBlank() && razorpayPlanId == null && planId == null
}

class AuthException(message: String) : Exception(message)

interface AuthService {
    /** Signed-in account, or null. Backed by Compose state so the UI follows it. */
    val account: Account?
    /** "Firebase" or "This device only". Shown on the login screen. */
    val backendLabel: String
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(name: String, email: String, password: String, referralCode: String? = null)
    suspend fun sendPasswordReset(email: String)
    fun signOut()
    /** Deletes the account and its server record. The caller clears data on the phone. */
    suspend fun deleteAccount()
}

interface UserDirectory {
    /** Writes the user-owned fields of [s]; never touches compPro / blocked / isAdmin. */
    suspend fun publish(s: UserSummary)
    suspend fun get(uid: String): UserSummary?
    suspend fun all(): List<UserSummary>
    suspend fun setCompPro(uid: String, value: Boolean)
    suspend fun setBlocked(uid: String, value: Boolean)
    suspend fun delete(uid: String)
    /** Admin: records that everything earned so far has been paid to the user. */
    suspend fun markReferralPaid(uid: String)
}

/** Most a user earns per friend who buys Pro with their code. Must match functions/lib.js. */
const val REFERRAL_REWARD_RUPEES = 500

/** On a monthly plan the reward comes in two parts: after the 1st and after the 2nd payment. */
const val REFERRAL_MONTHLY_INSTALMENT = 250

/** Total a friend's payments have earned so far: yearly ₹500; monthly ₹250 per payment, up to ₹500. */
fun referralEarnedFor(plan: Plan, payments: Int): Int = when (plan) {
    Plan.YEARLY -> REFERRAL_REWARD_RUPEES
    Plan.MONTHLY -> minOf(REFERRAL_REWARD_RUPEES, REFERRAL_MONTHLY_INSTALMENT * maxOf(payments, 1))
}

/**
 * A stable, shareable code for a user, e.g. "RTK7P2QX". No 0/O/1/I to avoid mix-ups.
 * [attempt] 0 is the usual code; later attempts give other codes for when one is already taken.
 */
fun referralCodeFor(uid: String, attempt: Int = 0): String {
    val alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    var h = 1125899906842597L
    for (c in if (attempt == 0) uid else "$uid#$attempt") h = 31 * h + c.code
    val sb = StringBuilder("RT")
    var x = h and Long.MAX_VALUE
    repeat(6) {
        sb.append(alphabet[(x % alphabet.length).toInt()])
        x /= alphabet.length
    }
    return sb.toString()
}

/** How many codes to try before giving up; a clash on all of them is practically impossible. */
const val REFERRAL_CODE_ATTEMPTS = 20

/** The user's first code that nobody else holds. [ownerOf] returns who holds a code, or null if it's free. */
fun firstFreeReferralCode(uid: String, ownerOf: (String) -> String?): String {
    for (attempt in 0 until REFERRAL_CODE_ATTEMPTS) {
        val code = referralCodeFor(uid, attempt)
        val owner = ownerOf(code)
        if (owner == null || owner == uid) return code
    }
    throw IllegalStateException("No free referral code")
}

fun normalizeReferralCode(code: String?): String? =
    code?.trim()?.uppercase()?.replace(" ", "")?.takeIf { it.isNotEmpty() }

interface Billing {
    /** Currently owned subscription, if any. Compose state. */
    val activePlan: Plan?
    /** Localised price per plan from the store, falling back to [Plan.fallbackPrice]. */
    fun price(plan: Plan): String
    /** First-year price with a referral code, or null when the store doesn't offer one. */
    fun referralPrice(plan: Plan): String? = plan.referralPrice
    /** Non-null when purchases can't happen right now (e.g. not installed from Google Play). */
    val unavailableReason: String?
    /** True when Google Play's user choice screen also offers Razorpay. */
    val offersAlternative: Boolean get() = false
    /** [referral]: buy at the first-year referral price (see [referralPrice]). */
    fun purchase(plan: Plan, referral: Boolean = false)
    fun restore()
}

/**
 * Razorpay subscriptions. The app never sees the key secret: the server
 * creates the subscription and verifies the payment signature.
 */
interface RazorpayGateway {
    /** Non-null when Razorpay can't be used (e.g. Firebase isn't set up). */
    val unavailableReason: String?
    /** True while a payment or cancellation is in progress. Compose state. */
    val busy: Boolean
    /** Last success or error message for the user. Compose state. */
    val message: String?
    /** Changes after every successful payment or cancellation, so the UI can reload the account. */
    val version: Int

    /**
     * Creates a subscription on the server, opens Razorpay Checkout and verifies
     * the result. [externalTransactionToken] comes from Google Play's user choice screen.
     */
    suspend fun subscribe(plan: Plan, account: Account, externalTransactionToken: String? = null)

    /** Turns off auto-renew; Pro stays until the paid period ends. */
    suspend fun cancel()
}

/** Used when there is no payments server (no Firebase config). */
class NoRazorpay(override val unavailableReason: String = t("Razorpay needs the online (Firebase) setup.")) : RazorpayGateway {
    override val busy = false
    override val message: String? = null
    override val version = 0
    override suspend fun subscribe(plan: Plan, account: Account, externalTransactionToken: String?) =
        throw AuthException(unavailableReason)
    override suspend fun cancel() = throw AuthException(unavailableReason)
}

fun validateEmail(email: String): String? =
    if (Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())) null else t("Enter a valid email address")

fun validatePassword(password: String): String? = when {
    password.length < 8 -> t("Use at least 8 characters")
    !password.any { it.isDigit() } || !password.any { it.isLetter() } -> t("Use letters and numbers")
    else -> null
}
