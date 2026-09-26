package com.rotitrack.app.account

import kotlinx.serialization.Serializable

data class Account(val uid: String, val name: String, val email: String, val isAdmin: Boolean)

/** Play Console product IDs. Create two auto-renewing subscriptions with these IDs. */
enum class Plan(val productId: String, val label: String, val fallbackPrice: String, val period: String, val rupees: Int) {
    MONTHLY("rotitrack_pro_monthly", "Monthly", "₹259", "month", 259),
    YEARLY("rotitrack_pro_yearly", "Yearly", "₹990", "year", 990);

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
) {
    /** Razorpay plan that is paid up right now; stays until the cycle ends even if auto-renew is off. */
    val razorpayPlan: Plan? get() = if (razorpayUntil > System.currentTimeMillis()) Plan.byProductId(razorpayPlanId) else null
    val plan: Plan? get() = Plan.byProductId(planId) ?: razorpayPlan
    val paidVia: String? get() = when {
        Plan.byProductId(planId) != null -> "Google Play"
        razorpayPlan != null -> "Razorpay"
        else -> null
    }
    val isPro: Boolean get() = plan != null || compPro
}

class AuthException(message: String) : Exception(message)

interface AuthService {
    /** Signed-in account, or null. Backed by Compose state so the UI follows it. */
    val account: Account?
    /** "Firebase" or "This device only". Shown on the login screen. */
    val backendLabel: String
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(name: String, email: String, password: String)
    suspend fun sendPasswordReset(email: String)
    fun signOut()
}

interface UserDirectory {
    /** Writes the user-owned fields of [s]; never touches compPro / blocked / isAdmin. */
    suspend fun publish(s: UserSummary)
    suspend fun get(uid: String): UserSummary?
    suspend fun all(): List<UserSummary>
    suspend fun setCompPro(uid: String, value: Boolean)
    suspend fun setBlocked(uid: String, value: Boolean)
}

interface Billing {
    /** Currently owned subscription, if any. Compose state. */
    val activePlan: Plan?
    /** Localised price per plan from the store, falling back to [Plan.fallbackPrice]. */
    fun price(plan: Plan): String
    /** Non-null when purchases can't happen right now (e.g. not installed from Google Play). */
    val unavailableReason: String?
    /** True when Google Play's user choice screen also offers Razorpay. */
    val offersAlternative: Boolean get() = false
    fun purchase(plan: Plan)
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
class NoRazorpay(override val unavailableReason: String = "Razorpay needs the online (Firebase) setup.") : RazorpayGateway {
    override val busy = false
    override val message: String? = null
    override val version = 0
    override suspend fun subscribe(plan: Plan, account: Account, externalTransactionToken: String?) =
        throw AuthException(unavailableReason)
    override suspend fun cancel() = throw AuthException(unavailableReason)
}

fun validateEmail(email: String): String? =
    if (Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$").matches(email.trim())) null else "Enter a valid email address"

fun validatePassword(password: String): String? = when {
    password.length < 8 -> "Use at least 8 characters"
    !password.any { it.isDigit() } || !password.any { it.isLetter() } -> "Use letters and numbers"
    else -> null
}
