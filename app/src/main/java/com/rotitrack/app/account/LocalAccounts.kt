package com.rotitrack.app.account

import com.rotitrack.app.i18n.t
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rotitrack.app.store.Storage
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import java.util.UUID
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
private data class LocalUser(
    val uid: String,
    val name: String,
    val email: String,
    val salt: String,
    val hash: String,
    val isAdmin: Boolean,
    val createdAt: Long,
)

@Serializable
private data class LocalAuthState(val users: List<LocalUser> = emptyList(), val sessionUid: String? = null)

/**
 * Accounts kept on this phone, used when Firebase isn't configured. Passwords
 * are stored as salted PBKDF2 hashes. The first account created is the admin.
 */
class LocalAuth(private val storage: Storage, private val directory: LocalDirectory) : AuthService {
    private var state = storage.load()?.let { runCatching { json.decodeFromString(LocalAuthState.serializer(), it) }.getOrNull() }
        ?: LocalAuthState()

    override var account: Account? by mutableStateOf(state.users.firstOrNull { it.uid == state.sessionUid }?.toAccount())
        private set

    override val backendLabel = "This device only"

    private fun save() = storage.save(json.encodeToString(LocalAuthState.serializer(), state))

    override suspend fun signIn(email: String, password: String) {
        val u = state.users.firstOrNull { it.email.equals(email.trim(), ignoreCase = true) }
        if (u == null || !MessageDigest.isEqual(hash(password, u.salt).toByteArray(), u.hash.toByteArray())) {
            throw AuthException(t("Email or password is incorrect"))
        }
        if (directory.get(u.uid)?.blocked == true) throw AuthException(t("This account has been blocked. Contact support."))
        state = state.copy(sessionUid = u.uid)
        save()
        account = u.toAccount()
    }

    override suspend fun signUp(name: String, email: String, password: String, referralCode: String?) {
        validateEmail(email)?.let { throw AuthException(it) }
        validatePassword(password)?.let { throw AuthException(it) }
        if (state.users.any { it.email.equals(email.trim(), ignoreCase = true) }) throw AuthException(t("An account with this email already exists"))
        normalizeReferralCode(referralCode)?.let { code ->
            if (directory.all().none { it.referralCode == code }) throw AuthException(t("That referral code doesn't exist. Check it or leave it empty."))
        }
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }.let { Base64.getEncoder().encodeToString(it) }
        val u = LocalUser(UUID.randomUUID().toString(), name.trim(), email.trim().lowercase(), salt, hash(password, salt),
            isAdmin = state.users.isEmpty(), createdAt = System.currentTimeMillis())
        state = state.copy(users = state.users + u, sessionUid = u.uid)
        save()
        val code = normalizeReferralCode(referralCode)
        val holders = directory.all().associate { it.referralCode to it.uid }
        directory.create(
            UserSummary(
                u.uid, u.name, u.email, createdAt = u.createdAt, lastActive = u.createdAt, isAdmin = u.isAdmin,
                referralCode = firstFreeReferralCode(u.uid) { holders[it] }, referredByCode = code,
            )
        )
        account = u.toAccount()
    }

    override suspend fun sendPasswordReset(email: String) {
        throw AuthException(t("Password reset needs the online (Firebase) setup. On this device, create a new account instead."))
    }

    override fun signOut() {
        state = state.copy(sessionUid = null)
        save()
        account = null
    }

    override suspend fun deleteAccount() {
        val uid = account?.uid ?: return
        directory.delete(uid)
        state = state.copy(users = state.users.filterNot { it.uid == uid }, sessionUid = null)
        save()
        account = null
    }

    private fun LocalUser.toAccount() = Account(uid, name, email, isAdmin)

    private fun hash(password: String, salt: String): String {
        val spec = PBEKeySpec(password.toCharArray(), Base64.getDecoder().decode(salt), 120_000, 256)
        val bytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        return Base64.getEncoder().encodeToString(bytes)
    }
}

/** Device-only counterpart of the Firestore `users` collection. */
class LocalDirectory(private val storage: Storage) : UserDirectory {
    private val serializer = MapSerializer(String.serializer(), UserSummary.serializer())
    private var users: Map<String, UserSummary> =
        storage.load()?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyMap()

    private fun save() = storage.save(json.encodeToString(serializer, users))

    fun create(s: UserSummary) {
        users = users + (s.uid to s)
        save()
    }

    override suspend fun publish(s: UserSummary) {
        val old = users[s.uid] ?: UserSummary(s.uid)
        var merged = s.copy(
            compPro = old.compPro, blocked = old.blocked, isAdmin = old.isAdmin,
            razorpayPlanId = old.razorpayPlanId, razorpayStatus = old.razorpayStatus, razorpayUntil = old.razorpayUntil,
            referredByCode = old.referredByCode, referralEarnings = old.referralEarnings,
            referralCount = old.referralCount, referralCredited = old.referralCredited, referralPaid = old.referralPaid,
            referralCreditedAmount = old.referralCreditedAmount,
            createdAt = if (old.createdAt > 0) old.createdAt else s.createdAt,
            // The code is chosen once, at sign-up.
            referralCode = old.referralCode,
        )
        // Stand-in for the server, which pays as each payment is confirmed. On this device there are
        // no renewals, so a paid plan counts as one payment: ₹500 for yearly, ₹250 for monthly.
        val referrer = merged.referredByCode?.let { code -> users.values.firstOrNull { it.referralCode == code } }
        val plan = merged.plan
        if (plan != null && referrer != null && referrer.uid != merged.uid) {
            val owed = referralEarnedFor(plan, payments = 1) - merged.referralCreditedAmount
            if (owed > 0) {
                users = users + (referrer.uid to referrer.copy(
                    referralEarnings = referrer.referralEarnings + owed,
                    referralCount = referrer.referralCount + if (merged.referralCreditedAmount == 0) 1 else 0,
                ))
                merged = merged.copy(referralCredited = true, referralCreditedAmount = merged.referralCreditedAmount + owed)
            }
        }
        users = users + (s.uid to merged)
        save()
    }

    override suspend fun markReferralPaid(uid: String) = edit(uid) { it.copy(referralPaid = it.referralEarnings) }

    override suspend fun delete(uid: String) {
        users = users - uid
        save()
    }

    override suspend fun get(uid: String) = users[uid]
    override suspend fun all() = users.values.sortedByDescending { it.createdAt }

    override suspend fun setCompPro(uid: String, value: Boolean) = edit(uid) { it.copy(compPro = value) }
    override suspend fun setBlocked(uid: String, value: Boolean) = edit(uid) { it.copy(blocked = value) }

    private fun edit(uid: String, f: (UserSummary) -> UserSummary) {
        users[uid]?.let { users = users + (uid to f(it)); save() }
    }
}

/**
 * Stand-in when Google Play Billing can't be reached (debug builds, emulators
 * without Play, or the preview). Purchases unlock Pro locally, per account, with no charge.
 */
class DemoBilling(private val storage: Storage, private val uid: () -> String?) : Billing {
    private val serializer = MapSerializer(String.serializer(), String.serializer())
    private var owned: Map<String, String> =
        storage.load()?.let { runCatching { json.decodeFromString(serializer, it) }.getOrNull() } ?: emptyMap()
    private var tick by mutableStateOf(0)

    override val activePlan: Plan?
        get() = tick.let { uid()?.let { u -> Plan.byProductId(owned[u]) } }

    override fun price(plan: Plan) = plan.fallbackPrice
    override val unavailableReason: String? = null

    override fun purchase(plan: Plan) {
        val u = uid() ?: return
        owned = owned + (u to plan.productId)
        storage.save(json.encodeToString(serializer, owned))
        tick++
    }

    override fun restore() { tick++ }

    /** Only for the demo: lets the Pro screen offer "cancel" so both states can be tried. */
    fun cancel() {
        val u = uid() ?: return
        owned = owned - u
        storage.save(json.encodeToString(serializer, owned))
        tick++
    }
}

