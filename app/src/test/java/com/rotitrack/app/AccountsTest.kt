package com.rotitrack.app

import com.rotitrack.app.account.AuthException
import com.rotitrack.app.account.DemoBilling
import com.rotitrack.app.account.LocalAuth
import com.rotitrack.app.account.LocalDirectory
import com.rotitrack.app.account.Plan
import com.rotitrack.app.account.UserSummary
import com.rotitrack.app.account.validatePassword
import com.rotitrack.app.store.Storage
import com.rotitrack.app.account.AdminStats
import com.rotitrack.app.account.yearlySavingPercent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import com.rotitrack.app.account.referralCodeProblem
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class AccountsTest {
    private class Mem(var text: String? = null) : Storage {
        override fun load() = text
        override fun save(text: String) { this.text = text }
        override fun clear() { text = null }
    }

    private fun expectAuthError(block: suspend () -> Unit): String = runBlocking {
        try {
            block()
            fail("expected AuthException")
            ""
        } catch (e: AuthException) {
            e.message.orEmpty()
        }
    }

    @Test fun signUpSignOutSignIn() = runBlocking {
        val authStore = Mem()
        val dir = LocalDirectory(Mem())
        val auth = LocalAuth(authStore, dir)
        auth.signUp("Asha Rao", "asha@example.com", "chai1234")
        val uid = auth.account!!.uid
        assertTrue("first account is admin", auth.account!!.isAdmin)
        auth.signOut()
        assertNull(auth.account)

        // A fresh instance reads the same saved accounts, like after an app restart.
        val again = LocalAuth(authStore, dir)
        again.signIn("ASHA@example.com", "chai1234")
        assertEquals(uid, again.account!!.uid)
        assertFalse(authStore.text!!.contains("chai1234"))
    }

    @Test fun rejectsBadCredentialsAndDuplicates() {
        val dir = LocalDirectory(Mem())
        val auth = LocalAuth(Mem(), dir)
        runBlocking { auth.signUp("A", "a@example.com", "password1") }
        assertEquals("Email or password is incorrect", expectAuthError { auth.signIn("a@example.com", "wrong999") })
        assertTrue(expectAuthError { auth.signUp("B", "a@example.com", "password1") }.contains("already exists"))
        assertNotNull(validatePassword("short1"))
        assertNotNull(validatePassword("lettersonly"))
    }

    @Test fun secondAccountIsNotAdminAndCanBeBlocked() = runBlocking {
        val dir = LocalDirectory(Mem())
        val auth = LocalAuth(Mem(), dir)
        auth.signUp("Admin", "admin@example.com", "admin1234")
        auth.signOut()
        auth.signUp("User", "user@example.com", "user12345")
        val uid = auth.account!!.uid
        assertFalse(auth.account!!.isAdmin)
        auth.signOut()
        dir.setBlocked(uid, true)
        assertTrue(expectAuthError { auth.signIn("user@example.com", "user12345") }.contains("blocked"))
    }

    @Test fun publishKeepsAdminOnlyFields() = runBlocking {
        val dir = LocalDirectory(Mem())
        dir.create(UserSummary("u1", "U", "u@x.com", createdAt = 5))
        dir.setCompPro("u1", true)
        dir.publish(UserSummary("u1", "U", "u@x.com", createdAt = 99, streak = 4))
        val u = dir.get("u1")!!
        assertTrue(u.compPro)
        assertEquals(5, u.createdAt)
        assertEquals(4, u.streak)
    }

    @Test fun demoBillingIsPerAccount() {
        var uid: String? = "a"
        val billing = DemoBilling(Mem()) { uid }
        billing.purchase(Plan.YEARLY)
        assertEquals(Plan.YEARLY, billing.activePlan)
        uid = "b"
        assertNull(billing.activePlan)
    }

    @Test fun adminStatsAndPricing() {
        val now = 10 * 86_400_000L
        val users = listOf(
            UserSummary("1", planId = Plan.MONTHLY.productId, createdAt = now - 1000, lastActive = now - 1000),
            UserSummary("2", planId = Plan.MONTHLY.productId, createdAt = 0),
            UserSummary("3", planId = Plan.YEARLY.productId, createdAt = 0),
            UserSummary("4", compPro = true, createdAt = 0),
            UserSummary("5", blocked = true, createdAt = 0),
        )
        val s = AdminStats.of(users, now)
        assertEquals(5, s.total)
        assertEquals(4, s.pro)
        assertEquals(2 * 359 + 1099 / 12, s.mrr)
        assertEquals(1, s.new7d)
        assertEquals(1, s.active24h)
        assertEquals(1, s.comp)
        assertEquals(74, yearlySavingPercent)
    }

    @Test fun razorpayProLastsUntilThePaidPeriodEnds() {
        val later = System.currentTimeMillis() + 86_400_000L
        val paid = UserSummary("r", razorpayPlanId = Plan.YEARLY.productId, razorpayStatus = "cancelled", razorpayUntil = later)
        assertEquals(Plan.YEARLY, paid.plan)
        assertEquals("Razorpay", paid.paidVia)
        assertTrue(paid.isPro)
        val expired = paid.copy(razorpayUntil = System.currentTimeMillis() - 1)
        assertNull(expired.plan)
        assertFalse(expired.isPro)
        // Revenue counts Razorpay subscribers too.
        assertEquals(Plan.YEARLY.rupees / 12, AdminStats.of(listOf(paid)).mrr)
    }

    @Test fun userCannotOverwriteServerRazorpayFields() = runBlocking {
        val dir = LocalDirectory(Mem())
        dir.create(UserSummary("u", razorpayPlanId = Plan.MONTHLY.productId, razorpayUntil = 42))
        dir.publish(UserSummary("u", razorpayPlanId = null, razorpayUntil = 0, streak = 3))
        assertEquals(42, dir.get("u")!!.razorpayUntil)
        assertEquals(Plan.MONTHLY.productId, dir.get("u")!!.razorpayPlanId)
    }

    @Test fun referralReportShowsWhoReferredWhomAndWhatIsDue() {
        val users = listOf(
            UserSummary("p1", name = "Asha", referralCode = "RTAAAAAA", referralEarnings = 750, referralPaid = 250, payoutUpi = "asha@upi"),
            UserSummary("p2", name = "Ravi", referralCode = "RTBBBBBB", referralEarnings = 500, referralPaid = 500),
            UserSummary("f1", name = "Krish", referralCode = "RTCCCCCC", referredByCode = "RTAAAAAA", referralCreditedAmount = 500, createdAt = 2),
            UserSummary("f2", name = "Meera", referralCode = "RTDDDDDD", referredByCode = "RTAAAAAA", referralCreditedAmount = 250, createdAt = 3),
            UserSummary("f3", name = "Sam", referralCode = "RTEEEEEE", referredByCode = "RTBBBBBB", referralCreditedAmount = 500, createdAt = 1),
            UserSummary("f4", name = "Lost", referralCode = "RTFFFFFF", referredByCode = "RTZZZZZZ", createdAt = 4),
            UserSummary("n", name = "Nobody", referralCode = "RTGGGGGG"),
        )
        val r = com.rotitrack.app.account.ReferralReport.of(users)
        assertEquals(listOf("f4", "f2", "f1", "f3"), r.links.map { it.friend.uid })
        assertEquals("Asha", r.promoterOf(users[2])?.name)
        assertNull(r.promoterOf(users[5]))
        assertEquals(listOf("p1", "p2"), r.promoters.map { it.user.uid })
        assertEquals(listOf("f2", "f1"), r.promoters[0].friends.map { it.uid })
        assertEquals(listOf("p1"), r.payoutsDue.map { it.user.uid })
        assertEquals(1250, r.earned)
        assertEquals(750, r.paid)
        assertEquals(500, r.due)
        assertEquals(3, r.wentPro)
    }

    @Test fun yearlyIsCheaperForTheFirstYearWithAReferralCode() {
        assertEquals(1099, Plan.YEARLY.rupees)
        assertEquals("₹990", Plan.YEARLY.referralPrice)
        assertEquals(null, Plan.MONTHLY.referralPrice)
        val me = UserSummary("u1", "Asha", "asha@example.com")
        assertTrue(me.firstSubscription)
        assertFalse(me.copy(razorpayPlanId = Plan.YEARLY.productId).firstSubscription)
        assertFalse(me.copy(planId = Plan.YEARLY.productId).firstSubscription)
        // A code entered on the yearly plan's payment page.
        val owners = mapOf("RTFRIEND1" to "u2", "RTMINE123" to "u1")
        val check = { code: String?, who: UserSummary -> runBlocking { referralCodeProblem(code, who) { owners[it] } } }
        assertEquals(null, check(" rtfriend1 ", me))
        assertNotNull(check("RTNOBODY9", me))
        assertNotNull(check("RTMINE123", me))
        assertNotNull(check("", me))
        assertNotNull(check("RTFRIEND1", me.copy(razorpayPlanId = Plan.MONTHLY.productId)))
    }
}
