package com.rotitrack.app.cloud

import com.rotitrack.app.i18n.t
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.SetOptions
import com.rotitrack.app.account.Account
import com.rotitrack.app.account.AuthException
import com.rotitrack.app.account.AuthService
import com.rotitrack.app.account.UserDirectory
import com.rotitrack.app.account.UserSummary
import com.rotitrack.app.account.normalizeReferralCode
import com.rotitrack.app.account.REFERRAL_CODE_ATTEMPTS
import com.rotitrack.app.account.referralCodeFor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * Email/password accounts with Firebase Auth. A user is an admin when their
 * `users/{uid}` document has `role: "admin"` (set by hand in the Firebase console).
 */
class FirebaseAuthService(private val directory: FirestoreDirectory) : AuthService {
    private val auth = FirebaseAuth.getInstance()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override var account: Account? by mutableStateOf(auth.currentUser?.let { Account(it.uid, it.displayName.orEmpty(), it.email.orEmpty(), false) })
        private set

    override val backendLabel = "Firebase"

    /** While true, sign-up finishes its own setup before the app moves past the login screen. */
    private var signingUp = false

    init {
        auth.addAuthStateListener { a ->
            val user = a.currentUser
            if (user == null) account = null else if (!signingUp) scope.launch { refresh(user) }
        }
    }

    private suspend fun refresh(user: FirebaseUser, name: String? = null) {
        val doc = runCatching { directory.get(user.uid) }.getOrNull()
        account = Account(
            uid = user.uid,
            name = name ?: user.displayName ?: doc?.name.orEmpty(),
            email = user.email.orEmpty(),
            isAdmin = doc?.isAdmin == true,
        )
    }

    override suspend fun signIn(email: String, password: String) = friendly {
        val user = auth.signInWithEmailAndPassword(email, password).await().user ?: throw AuthException(t("Sign-in failed"))
        if (directory.get(user.uid)?.blocked == true) {
            auth.signOut()
            throw AuthException(t("This account has been blocked. Contact support."))
        }
        refresh(user)
    }

    override suspend fun signUp(name: String, email: String, password: String, referralCode: String?) = friendly {
        val code = normalizeReferralCode(referralCode)
        if (code != null && !directory.referralCodeExists(code)) {
            throw AuthException(t("That referral code doesn't exist. Check it or leave it empty."))
        }
        // Run in the service's scope so leaving the login screen can't cut the setup short.
        scope.async {
            signingUp = true
            try {
                val user = auth.createUserWithEmailAndPassword(email, password).await().user ?: throw AuthException(t("Sign-up failed"))
                runCatching { user.updateProfile(UserProfileChangeRequest.Builder().setDisplayName(name).build()).await() }
                // The account exists now; a failed profile write is retried by the app's next publish.
                runCatching { withTimeout(NETWORK_TIMEOUT_MS) { directory.create(user.uid, name, email, code) } }
                refresh(user, name)
            } finally {
                signingUp = false
            }
        }.await()
    }

    override suspend fun deleteAccount() = friendly {
        val user = auth.currentUser ?: return@friendly
        directory.delete(user.uid)
        try {
            user.delete().await()
        } catch (e: FirebaseAuthRecentLoginRequiredException) {
            throw AuthException(t("For your security, sign out, sign in again, then delete your account."))
        }
    }

    override suspend fun sendPasswordReset(email: String) = friendly {
        auth.sendPasswordResetEmail(email).await()
        Unit
    }

    override fun signOut() = auth.signOut()

    /** Turns Firebase's exceptions into messages a user can act on, and gives up on a stalled network. */
    private suspend fun <T> friendly(block: suspend () -> T): T = try {
        withTimeout(2 * NETWORK_TIMEOUT_MS) { block() }
    } catch (e: TimeoutCancellationException) {
        throw AuthException(t("The server is taking too long. Check your internet connection and try again."))
    } catch (e: AuthException) {
        throw e
    } catch (e: FirebaseAuthWeakPasswordException) {
        throw AuthException(t("That password is too weak"))
    } catch (e: FirebaseAuthUserCollisionException) {
        throw AuthException(t("An account with this email already exists"))
    } catch (e: FirebaseAuthInvalidUserException) {
        throw AuthException(t("Email or password is incorrect"))
    } catch (e: FirebaseAuthInvalidCredentialsException) {
        throw AuthException(t("Email or password is incorrect"))
    } catch (e: FirebaseNetworkException) {
        throw AuthException(t("No internet connection"))
    } catch (e: FirebaseFirestoreException) {
        throw AuthException(
            if (e.code == FirebaseFirestoreException.Code.UNAVAILABLE) t("No internet connection")
            else t("Couldn't reach the server ({0}). Try again.", e.code.name.lowercase())
        )
    }

    private companion object {
        const val NETWORK_TIMEOUT_MS = 15_000L
    }
}

/** The `users` collection. See firestore.rules for who may write which fields. */
class FirestoreDirectory : UserDirectory {
    private val users = FirebaseFirestore.getInstance().collection("users")

    private val codes = FirebaseFirestore.getInstance().collection("referralCodes")
    private val db = FirebaseFirestore.getInstance()
    private var codeChecked = false

    suspend fun create(uid: String, name: String, email: String, referredByCode: String?) {
        val now = System.currentTimeMillis()
        val code = claimReferralCode(uid)
        users.document(uid).set(
            mapOf(
                "name" to name, "email" to email, "createdAt" to now, "lastActive" to now,
                "referralCode" to code, "referredByCode" to referredByCode,
            )
        ).await()
        codeChecked = true
    }

    suspend fun referralCodeExists(code: String): Boolean =
        withTimeout(15_000) { codes.document(code).get(Source.SERVER).await().exists() }

    /**
     * referralCodes/{code} → {uid} lets sign-up check a code and the server find the referrer.
     * Takes the user's first code nobody else holds, atomically, so two users never share one.
     */
    private suspend fun claimReferralCode(uid: String): String {
        for (attempt in 0 until REFERRAL_CODE_ATTEMPTS) {
            val code = referralCodeFor(uid, attempt)
            val ref = codes.document(code)
            val mine = db.runTransaction { t ->
                val snap = t.get(ref)
                when {
                    !snap.exists() -> { t.set(ref, mapOf("uid" to uid)); true }
                    else -> snap.getString("uid") == uid
                }
            }.await()
            if (mine) return code
        }
        throw IllegalStateException("No free referral code")
    }

    /** Once per session: make sure the user's saved code is registered to them (older accounts, interrupted sign-ups). */
    private suspend fun ensureReferralCode(uid: String) {
        if (codeChecked) return
        val saved = users.document(uid).get().await().getString("referralCode")
        val ok = saved != null && codes.document(saved).get().await().getString("uid") == uid
        if (!ok) {
            val code = claimReferralCode(uid)
            users.document(uid).set(mapOf("referralCode" to code), SetOptions.merge()).await()
        }
        codeChecked = true
    }

    override suspend fun publish(s: UserSummary) {
        users.document(s.uid).set(
            mapOf(
                "name" to s.name,
                "email" to s.email,
                "lastActive" to s.lastActive,
                "planId" to s.planId,
                "calorieGoal" to s.calorieGoal,
                "waterGoalMl" to s.waterGoalMl,
                "streak" to s.streak,
                "daysLogged" to s.daysLogged,
                "diet" to s.diet,
                "payoutUpi" to s.payoutUpi,
            ),
            SetOptions.merge(),
        ).await()
        runCatching { ensureReferralCode(s.uid) }
    }

    override suspend fun get(uid: String): UserSummary? = users.document(uid).get().await().toSummary()

    override suspend fun all(): List<UserSummary> =
        users.get().await().documents.mapNotNull { it.toSummary() }.sortedByDescending { it.createdAt }

    override suspend fun setCompPro(uid: String, value: Boolean) {
        users.document(uid).update("compPro", value).await()
    }

    override suspend fun setBlocked(uid: String, value: Boolean) {
        users.document(uid).update("blocked", value).await()
    }

    override suspend fun markReferralPaid(uid: String) {
        val earned = users.document(uid).get().await().getLong("referralEarnings") ?: 0
        users.document(uid).update("referralPaid", earned).await()
    }

    override suspend fun delete(uid: String) {
        users.document(uid).delete().await()
    }

    private fun DocumentSnapshot.toSummary(): UserSummary? = if (!exists()) null else UserSummary(
        uid = id,
        name = getString("name").orEmpty(),
        email = getString("email").orEmpty(),
        createdAt = getLong("createdAt") ?: 0,
        lastActive = getLong("lastActive") ?: 0,
        planId = getString("planId"),
        compPro = getBoolean("compPro") ?: false,
        blocked = getBoolean("blocked") ?: false,
        isAdmin = getString("role") == "admin",
        calorieGoal = getLong("calorieGoal")?.toInt() ?: 0,
        waterGoalMl = getLong("waterGoalMl")?.toInt() ?: 0,
        streak = getLong("streak")?.toInt() ?: 0,
        daysLogged = getLong("daysLogged")?.toInt() ?: 0,
        diet = getString("diet").orEmpty(),
        razorpayPlanId = getString("razorpayPlanId"),
        razorpayStatus = getString("razorpayStatus"),
        razorpayUntil = getLong("razorpayUntil") ?: 0,
        referralCode = getString("referralCode").orEmpty(),
        referredByCode = getString("referredByCode"),
        referralEarnings = getLong("referralEarnings")?.toInt() ?: 0,
        referralCount = getLong("referralCount")?.toInt() ?: 0,
        referralCredited = getBoolean("referralCredited") ?: false,
        referralCreditedAmount = getLong("referralCreditedAmount")?.toInt() ?: 0,
        referralPaid = getLong("referralPaid")?.toInt() ?: 0,
        payoutUpi = getString("payoutUpi").orEmpty(),
    )
}
