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
    YEARLY("rotitrack_pro_yearly", "Yearly", "₹1,099", "year", 1099, referralRupees = 999);

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
    /** This user's own code to share, e.g. "PRIYA100". */
    val referralCode: String = "",
    /** The code this user signed up with, if any. Set once, at sign-up. */
    val referredByCode: String? = null,
    /** Server-maintained: ₹ earned and friends who went Pro with this user's code. */
    val referralEarnings: Int = 0,
    val referralCount: Int = 0,
    /** Server-maintained: this user's own Pro purchase has already paid out their referrer. */
    val referralCredited: Boolean = false,
    /** ₹ this user's payments have earned their referrer so far (0, 150 or 300). */
    val referralCreditedAmount: Int = 0,
    /** Where referral earnings should be paid. */
    val payoutUpi: String = "",
    /** Admin-maintained: ₹ of [referralEarnings] already paid out. */
    val referralPaid: Int = 0,
    /** Server-maintained: ₹ of [referralEarnings] that came from monthly bonuses. */
    val referralBonusEarnings: Int = 0,
    /** Server-maintained: the month ("2026-11", India time) of the last yearly sale, its sale count and bonus. */
    val referralMonth: String = "",
    val referralMonthYearly: Int = 0,
    val referralMonthBonus: Int = 0,
    /** Server-maintained: month ("2026-11", India time) → yearly sales with this user's code and the bonus paid. */
    val referralMonths: Map<String, ReferralMonth> = emptyMap(),
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

    /** Has never subscribed, so a referral code can still give the first-year price. */
    val firstSubscription: Boolean get() = razorpayPlanId == null && planId == null
}

/** One month of a promoter's referrals: yearly plans sold with their code, and the bonus reached. */
@kotlinx.serialization.Serializable
data class ReferralMonth(val yearly: Int = 0, val bonus: Int = 0)

/**
 * Why [code] can't give [me] the yearly plan's referral price, or null when it can.
 * The payments server checks the same things again before charging.
 */
suspend fun referralCodeProblem(code: String?, me: UserSummary?, ownerOf: suspend (String) -> String?): String? {
    val c = normalizeReferralCode(code) ?: return t("Enter a referral code")
    if (me != null && !me.firstSubscription) return t("The referral price is only for your first Pro subscription.")
    val owner = runCatching { ownerOf(c) }.getOrElse { return t("Couldn't check the code. Check your internet and try again.") }
        ?: return t("That referral code doesn't exist. Check it and try again.")
    if (owner == me?.uid) return t("You can't use your own referral code.")
    return null
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
    /** Whether [signInWithGoogle] works with this backend. */
    val supportsGoogle: Boolean get() = false
    /**
     * Signs in with a Google ID token, creating the account on first use.
     * [referralCode] only counts for a new account.
     */
    suspend fun signInWithGoogle(idToken: String, referralCode: String? = null) {
        throw AuthException(t("Google sign-in isn't available. Use email and password."))
    }
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
    /** The uid that owns a referral code, or null if no one has it. */
    suspend fun referralCodeOwner(code: String): String?
}

/** Most a user earns per friend who buys Pro with their code. Must match functions/lib.js. */
const val REFERRAL_REWARD_RUPEES = 300

/** On a monthly plan the reward comes in two parts: after the 1st and after the 2nd payment. */
const val REFERRAL_MONTHLY_INSTALMENT = 150

/**
 * Monthly bonus: yearly sales made with the user's code in one calendar month (India time) →
 * bonus for that month. Only the highest level reached counts. Must match functions/lib.js.
 */
val REFERRAL_BONUS_LEVELS: List<Pair<Int, Int>> = listOf(20 to 2000, 30 to 3500, 40 to 5000, 50 to 7000, 75 to 8500, 100 to 10000)

fun referralBonusFor(yearlySales: Int): Int = REFERRAL_BONUS_LEVELS.lastOrNull { yearlySales >= it.first }?.second ?: 0

/** The next bonus level above [yearlySales], or null once the top level is reached. */
fun nextReferralBonusLevel(yearlySales: Int): Pair<Int, Int>? = REFERRAL_BONUS_LEVELS.firstOrNull { yearlySales < it.first }

/** "2026-11" for [ms] in India time, the month the bonus counts in. Must match istMonthKey in functions/lib.js. */
fun istMonthKey(ms: Long): String {
    val d = java.time.Instant.ofEpochMilli(ms).atOffset(java.time.ZoneOffset.ofHoursMinutes(5, 30))
    return "%d-%02d".format(d.year, d.monthValue)
}

/** Total a friend's payments have earned so far: yearly ₹300; monthly ₹150 per payment, up to ₹300. */
fun referralEarnedFor(plan: Plan, payments: Int): Int = when (plan) {
    Plan.YEARLY -> REFERRAL_REWARD_RUPEES
    Plan.MONTHLY -> minOf(REFERRAL_REWARD_RUPEES, REFERRAL_MONTHLY_INSTALMENT * maxOf(payments, 1))
}

/**
 * The letters of a referral code, from the user's name: the first word with at least three
 * English letters ("K Erappa" → "ERAPPA"), at most 8 letters, or "ROTI" when there's none.
 */
fun referralCodeName(name: String): String {
    val words = name.uppercase().split(Regex("[^A-Z]+")).filter { it.isNotEmpty() }
    val word = words.firstOrNull { it.length >= 3 } ?: words.joinToString("").takeIf { it.length >= 3 }
    return (word ?: "ROTI").take(8)
}

/**
 * A memorable, shareable code, e.g. "PRIYA100". [attempt] 0 is the usual code; later attempts
 * give PRIYA101… and then other numbers, for when one is already taken.
 */
fun referralCodeFor(name: String, uid: String, attempt: Int = 0): String {
    val number = if (attempt < 10) 100 + attempt else {
        var h = 1125899906842597L
        for (c in "$uid#$attempt") h = 31 * h + c.code
        110 + ((h and Long.MAX_VALUE) % 890).toInt()
    }
    return referralCodeName(name) + number
}

/** How many codes to try before giving up; a clash on all of them is practically impossible. */
const val REFERRAL_CODE_ATTEMPTS = 40

/** The user's first code that nobody else holds. [ownerOf] returns who holds a code, or null if it's free. */
fun firstFreeReferralCode(name: String, uid: String, ownerOf: (String) -> String?): String {
    for (attempt in 0 until REFERRAL_CODE_ATTEMPTS) {
        val code = referralCodeFor(name, uid, attempt)
        val owner = ownerOf(code)
        if (owner == null || owner == uid) return code
    }
    throw IllegalStateException("No free referral code")
}

/** Codes from before name codes looked like "RTK7P2QX"; they're replaced by a name code. */
fun isNameReferralCode(code: String): Boolean = Regex("[A-Z]{3,8}[0-9]{3}").matches(code)

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
    /** [referralCode]: a friend's code, already checked, for the first-year referral price (see [referralPrice]). */
    fun purchase(plan: Plan, referralCode: String? = null)
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
    /** [referralCode]: a friend's code entered before paying, for the yearly plan's first-year price. */
    suspend fun subscribe(plan: Plan, account: Account, externalTransactionToken: String? = null, referralCode: String? = null)

    /** Turns off auto-renew; Pro stays until the paid period ends. */
    suspend fun cancel()
}

/** Used when there is no payments server (no Firebase config). */
class NoRazorpay(override val unavailableReason: String = t("Razorpay needs the online (Firebase) setup.")) : RazorpayGateway {
    override val busy = false
    override val message: String? = null
    override val version = 0
    override suspend fun subscribe(plan: Plan, account: Account, externalTransactionToken: String?, referralCode: String?) =
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
