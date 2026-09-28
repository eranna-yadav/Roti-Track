package com.rotitrack.app

import com.rotitrack.app.account.AuthException
import com.rotitrack.app.account.DemoBilling
import com.rotitrack.app.account.LocalAuth
import com.rotitrack.app.account.LocalDirectory
import com.rotitrack.app.account.Plan
import com.rotitrack.app.account.REFERRAL_MONTHLY_INSTALMENT
import com.rotitrack.app.account.REFERRAL_REWARD_RUPEES
import com.rotitrack.app.account.referralEarnedFor
import com.rotitrack.app.account.UserSummary
import com.rotitrack.app.account.referralCodeFor
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.data.builtinFood
import com.rotitrack.app.data.exerciseById
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.Reports
import com.rotitrack.app.store.AppStore
import com.rotitrack.app.store.Storage
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ProfileFeaturesTest {
    private class Mem(var text: String? = null) : Storage {
        override fun load() = text
        override fun save(text: String) { this.text = text }
        override fun clear() { text = null }
    }

    private fun store() = AppStore(Mem()).apply { updateProfile { it.copy(onboarded = true, weightKg = 70.0) } }

    @Test fun exerciseBurnsCaloriesAndCanRaiseTheGoal() {
        val s = store()
        val today = Days.today()
        s.addExercise(exerciseById("run"), 30, today)   // 9 MET × 70 kg × 0.5 h = 315
        assertEquals(315, s.burned(today))
        assertEquals(s.profile.calorieGoal, s.calorieGoalFor(today))
        s.updatePrefs { it.copy(addBurnedCalories = true) }
        assertEquals(s.profile.calorieGoal + 315, s.calorieGoalFor(today))
    }

    @Test fun rolloverIsCappedAt200AndNeedsFoodYesterday() {
        val s = store()
        val today = Days.today()
        val yesterday = Days.shift(today, -1)
        s.updatePrefs { it.copy(rolloverCalories = true) }
        assertEquals(0, s.rollover(today))
        s.logFood(builtinFood("roti")!!, 2.0, MealSlot.LUNCH, yesterday)   // 220 kcal, far under goal
        assertEquals(200, s.rollover(today))
        assertEquals(s.profile.calorieGoal + 200, s.calorieGoalFor(today))
    }

    @Test fun weightLogKeepsOneEntryPerDayAndUpdatesProfile() {
        val s = store()
        s.logWeight(71.0)
        s.logWeight(70.4)
        assertEquals(1, s.state.weights.size)
        assertEquals(70.4, s.profile.weightKg, 0.001)
    }

    @Test fun fastingRecordsHistory() {
        val s = store()
        s.setFastTarget(16)
        s.startFast(at = 0)
        s.endFast(at = 17 * 3_600_000L)
        assertNull(s.state.fasting.activeStart)
        assertEquals(1, s.state.fasting.history.size)
        assertTrue("first_fast" in s.state.badges)
    }

    @Test fun badgesUnlockOnceAndQueueACelebration() {
        val s = store()
        s.logFood(builtinFood("idli")!!, 3.0, MealSlot.BREAKFAST, Days.today())
        assertTrue("first_meal" in s.state.badges)
        assertEquals(listOf("first_meal"), s.celebrations.map { it.id })
        s.logFood(builtinFood("idli")!!, 1.0, MealSlot.BREAKFAST, Days.today())
        assertEquals(1, s.celebrations.size)
        s.updatePrefs { it.copy(badgeCelebrations = false) }
        s.addWater(250)
        assertTrue("first_water" in s.state.badges)
        assertEquals(1, s.celebrations.size)
    }

    @Test fun referralCodesAreStableAndReadable() {
        val a = referralCodeFor("user-123")
        assertEquals(a, referralCodeFor("user-123"))
        assertTrue(a.matches(Regex("RT[A-HJ-NP-Z2-9]{6}")))
        assertFalse(a == referralCodeFor("user-124"))
    }

    @Test fun referrerEarns500WhenFriendGoesPro() = runBlocking {
        val dir = LocalDirectory(Mem())
        val auth = LocalAuth(Mem(), dir)
        auth.signUp("Asha", "asha@example.com", "chai1234")
        val asha = auth.account!!.uid
        val code = referralCodeFor(asha)
        auth.signOut()

        try {
            auth.signUp("Ravi", "ravi@example.com", "dosa1234", "RTNOPE99")
            fail("unknown code should be rejected")
        } catch (e: AuthException) { /* expected */ }

        auth.signUp("Ravi", "ravi@example.com", "dosa1234", code.lowercase())
        val ravi = auth.account!!.uid
        assertEquals(code, dir.get(ravi)!!.referredByCode)

        // Signing up alone earns nothing…
        dir.publish(UserSummary(ravi, "Ravi", "ravi@example.com"))
        assertEquals(0, dir.get(asha)!!.referralEarnings)
        // …going Pro pays out once.
        dir.publish(UserSummary(ravi, "Ravi", "ravi@example.com", planId = Plan.MONTHLY.productId))
        dir.publish(UserSummary(ravi, "Ravi", "ravi@example.com", planId = Plan.MONTHLY.productId))
        // Monthly pays ₹250 for the first payment (the server adds ₹250 more on the second).
        assertEquals(REFERRAL_MONTHLY_INSTALMENT, dir.get(asha)!!.referralEarnings)
        // Switching to yearly pays the rest, and the friend still counts once.
        dir.publish(UserSummary(ravi, "Ravi", "ravi@example.com", planId = Plan.YEARLY.productId))
        dir.publish(UserSummary(ravi, "Ravi", "ravi@example.com", planId = Plan.YEARLY.productId))
        assertEquals(REFERRAL_REWARD_RUPEES, dir.get(asha)!!.referralEarnings)
        assertEquals(1, dir.get(asha)!!.referralCount)
        assertEquals(REFERRAL_REWARD_RUPEES, dir.get(asha)!!.referralDue)
        dir.markReferralPaid(asha)
        assertEquals(0, dir.get(asha)!!.referralDue)
    }

    @Test fun deleteAccountRemovesIt() = runBlocking {
        val dir = LocalDirectory(Mem())
        val auth = LocalAuth(Mem(), dir)
        auth.signUp("Asha", "asha@example.com", "chai1234")
        val uid = auth.account!!.uid
        auth.deleteAccount()
        assertNull(auth.account)
        assertNull(dir.get(uid))
        try {
            auth.signIn("asha@example.com", "chai1234")
            fail("deleted account should not sign in")
        } catch (e: AuthException) { /* expected */ }
    }

    @Test fun reportCoversTheRange() {
        val s = store()
        val today = Days.today()
        s.logFood(builtinFood("dal-tadka")!!, 1.0, MealSlot.LUNCH, today)
        s.logFood(builtinFood("rice")!!, 1.0, MealSlot.LUNCH, Days.shift(today, -40))
        s.addExercise(exerciseById("yoga"), 30, today)
        s.logWeight(69.5)
        val r = Reports.build(s, "Asha", 30)
        assertEquals(30, r.days.size)
        assertEquals(1, r.meals.size)
        assertEquals(1, r.exercises.size)
        assertEquals(1, r.weeklyWeights.size)
        assertEquals(180, r.avgKcal)
    }

    @Suppress("unused")
    private val billing = DemoBilling(Mem()) { null }
}

class ReferralCodeTest {
    @Test fun firstAttemptKeepsTheOriginalCode() {
        val uid = "abc123"
        org.junit.Assert.assertEquals(com.rotitrack.app.account.referralCodeFor(uid), com.rotitrack.app.account.referralCodeFor(uid, 0))
        org.junit.Assert.assertNotEquals(com.rotitrack.app.account.referralCodeFor(uid), com.rotitrack.app.account.referralCodeFor(uid, 1))
        org.junit.Assert.assertTrue(com.rotitrack.app.account.referralCodeFor(uid, 3).matches(Regex("RT[A-HJ-NP-Z2-9]{6}")))
    }

    @Test fun takenCodeMovesToTheNextOne() {
        val uid = "user-b"
        val first = com.rotitrack.app.account.referralCodeFor(uid)
        val code = com.rotitrack.app.account.firstFreeReferralCode(uid) { if (it == first) "user-a" else null }
        org.junit.Assert.assertEquals(com.rotitrack.app.account.referralCodeFor(uid, 1), code)
        // A code the user already holds is theirs.
        org.junit.Assert.assertEquals(first, com.rotitrack.app.account.firstFreeReferralCode(uid) { if (it == first) uid else null })
    }
}

class ReferralRewardTest {
    @Test fun monthlyPaysInTwoParts() {
        org.junit.Assert.assertEquals(250, referralEarnedFor(Plan.MONTHLY, 1))
        org.junit.Assert.assertEquals(500, referralEarnedFor(Plan.MONTHLY, 2))
        org.junit.Assert.assertEquals(500, referralEarnedFor(Plan.MONTHLY, 12))
        org.junit.Assert.assertEquals(500, referralEarnedFor(Plan.YEARLY, 1))
    }
}
