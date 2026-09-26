package com.sipwell.app

import com.sipwell.app.account.AuthException
import com.sipwell.app.account.DemoBilling
import com.sipwell.app.account.LocalAuth
import com.sipwell.app.account.LocalDirectory
import com.sipwell.app.account.Plan
import com.sipwell.app.account.UserSummary
import com.sipwell.app.account.validatePassword
import com.sipwell.app.store.Storage
import com.sipwell.app.account.AdminStats
import com.sipwell.app.account.yearlySavingPercent
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
        assertEquals(2 * 259 + 990 / 12, s.mrr)
        assertEquals(1, s.new7d)
        assertEquals(1, s.active24h)
        assertEquals(1, s.comp)
        assertEquals(68, yearlySavingPercent)
    }
}
