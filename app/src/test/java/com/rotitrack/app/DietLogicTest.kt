package com.rotitrack.app

import com.rotitrack.app.data.Activity
import com.rotitrack.app.data.Diet
import com.rotitrack.app.data.FOODS
import com.rotitrack.app.data.FoodCategory
import com.rotitrack.app.data.Gender
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.Region
import com.rotitrack.app.data.TEMPLATES
import com.rotitrack.app.data.WeightGoal
import com.rotitrack.app.data.builtinFood
import com.rotitrack.app.domain.Nutrition
import com.rotitrack.app.domain.Planner
import com.rotitrack.app.domain.formatServings
import com.rotitrack.app.i18n.I18n
import com.rotitrack.app.store.AppStore
import com.rotitrack.app.domain.Days
import com.rotitrack.app.store.Storage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class DietLogicTest {

    private class MemoryStorage(var text: String? = null) : Storage {
        override fun load() = text
        override fun save(text: String) { this.text = text }
        override fun clear() { text = null }
    }

    @Test fun foodIdsAreUnique() {
        assertEquals(FOODS.size, FOODS.map { it.id }.toSet().size)
    }

    @Test fun everyTemplateItemIsAKnownFood() {
        TEMPLATES.values.flatten().flatMap { it.items }.forEach { assertNotNull(it.foodId, builtinFood(it.foodId)) }
    }

    @Test fun dietPlansUseOnlyIndianFoods() {
        TEMPLATES.values.flatten().flatMap { it.items }.forEach {
            assertTrue(it.foodId, builtinFood(it.foodId)!!.category.indian)
        }
        for (diet in Diet.entries) for (slot in MealSlot.entries) for (shuffle in 0..20) {
            val meal = Planner.planMeal("2026-09-30", slot, shuffle, 2000, diet, Region.ALL, ::builtinFood)
            assertTrue(meal.name, meal.items.all { it.food.category.indian })
        }
    }

    @Test fun westernAndChineseListsExistAndAreTranslated() {
        for (c in listOf(FoodCategory.WESTERN, FoodCategory.CHINESE)) {
            val foods = FOODS.filter { it.category == c }
            assertTrue("$c", foods.size >= 20)
            for (code in listOf("hi", "kn", "te", "ta", "mr", "bn", "gu")) {
                val table = I18n.table(code)
                (foods.flatMap { listOf(it.name, it.serving) } + listOf("Western", "Chinese"))
                    .forEach { assertNotNull("$code: $it", table[it]) }
            }
        }
    }

    @Test fun dryFruitsSnacksAndSweetsAreListedAndTranslated() {
        assertTrue(FOODS.count { it.category == FoodCategory.DRY_FRUITS } >= 12)
        assertTrue(FOODS.count { it.category == FoodCategory.SNACKS } >= 30)
        assertTrue(FOODS.count { it.category == FoodCategory.SWEETS } >= 24)
        val foods = FOODS.filter { it.category in listOf(FoodCategory.DRY_FRUITS, FoodCategory.SNACKS, FoodCategory.SWEETS) }
        for (code in listOf("hi", "kn", "te", "ta", "mr", "bn", "gu")) {
            val table = I18n.table(code)
            (foods.flatMap { listOf(it.name, it.serving) } + "Dry fruits").forEach { assertNotNull("$code: $it", table[it]) }
        }
    }

    @Test fun calorieTargetFollowsMifflinStJeor() {
        // 70 kg, 172 cm, 30 y male: BMR 1630 × 1.375 = 2241.25, −500 → 1740.
        val p = Profile(gender = Gender.MALE, weightKg = 70.0, heightCm = 172, age = 30, goal = WeightGoal.LOSE)
        assertEquals(1740, Nutrition.recommendedCalories(p))
        assertEquals(1200, Nutrition.recommendedCalories(Profile(weightKg = 40.0, heightCm = 145, age = 70, goal = WeightGoal.LOSE)))
    }

    @Test fun macrosAddUpToTheTarget() {
        val m = Nutrition.macroTargets(2000, 65.0, WeightGoal.MAINTAIN)
        assertTrue(abs(m.protein * 4 + m.carbs * 4 + m.fat * 9 - 2000) <= 10)
    }

    @Test fun vegetarianPlansNeverContainEggOrMeat() {
        for (day in listOf("2026-01-01", "2026-05-17", "2026-09-25")) for (slot in MealSlot.entries) for (shuffle in 0..20) {
            val meal = Planner.planMeal(day, slot, shuffle, 1800, Diet.VEG, Region.ALL, ::builtinFood)
            assertTrue(meal.name, meal.items.all { it.food.diet == Diet.VEG })
        }
    }

    @Test fun plansLandNearTheTarget() {
        for (region in Region.entries) for (diet in Diet.entries) {
            val total = MealSlot.entries.sumOf { Planner.planMeal("2026-09-25", it, 0, 2000, diet, region, ::builtinFood).kcal }
            assertTrue("$region $diet gave $total", total in 1500..2500)
        }
    }

    @Test fun planIsStableAndSwapChangesIt() {
        val a = Planner.planMeal("2026-09-25", MealSlot.LUNCH, 0, 1800, Diet.NONVEG, Region.ALL, ::builtinFood)
        val b = Planner.planMeal("2026-09-25", MealSlot.LUNCH, 0, 1800, Diet.NONVEG, Region.ALL, ::builtinFood)
        val c = Planner.planMeal("2026-09-25", MealSlot.LUNCH, 1, 1800, Diet.NONVEG, Region.ALL, ::builtinFood)
        assertEquals(a, b)
        assertFalse(a.name == c.name)
    }

    @Test fun storeLogsPersistsAndRestores() {
        val storage = MemoryStorage()
        val store = AppStore(storage)
        store.updateProfile { it.copy(onboarded = true, weightKg = 70.0) }
        val roti = builtinFood("roti")!!
        store.logFood(roti, 2.0, MealSlot.LUNCH, "2026-09-25")
        store.addWater(500, "tea")

        val restored = AppStore(storage)
        assertEquals(220, restored.totals("2026-09-25").kcal)
        assertEquals(425, restored.waterTotal(com.rotitrack.app.domain.Days.today()))
        assertEquals(2450, restored.profile.waterGoalMl)
    }

    @Test fun waterGoalFollowsWeightGenderAndActivity() {
        // Drinks only: close to the ~3 L / ~2.2 L behind the 3.7 L / 2.7 L total-fluid advice.
        assertEquals(2800, Nutrition.recommendedWaterMl(70.0, Gender.MALE, Activity.SEDENTARY))
        assertEquals(1930, Nutrition.recommendedWaterMl(55.0, Gender.FEMALE, Activity.LIGHT))
        assertEquals(3050, Nutrition.recommendedWaterMl(70.0, Gender.MALE, Activity.MODERATE))
        assertEquals(2430, Nutrition.recommendedWaterMl(55.0, Gender.FEMALE, Activity.ATHLETE))
        assertEquals(5000, Nutrition.recommendedWaterMl(150.0, Gender.MALE, Activity.ACTIVE))
    }

    @Test fun loggingAPlannedMealMarksItLogged() {
        val store = AppStore(MemoryStorage())
        val meal = store.plan("2026-09-25").first()
        assertFalse(store.isPlanLogged("2026-09-25", meal.slot))
        store.logPlannedMeal("2026-09-25", meal)
        assertTrue(store.isPlanLogged("2026-09-25", meal.slot))
        assertEquals(meal.kcal.toDouble(), store.totals("2026-09-25").kcal.toDouble(), meal.items.size.toDouble())
    }

    @Test fun servingsFormatting() {
        assertEquals("½", formatServings(0.5))
        assertEquals("2", formatServings(2.0))
        assertEquals("1½", formatServings(1.5))
    }

    @Test fun corruptSaveFallsBackToDefaults() {
        val store = AppStore(MemoryStorage("{not json"))
        assertFalse(store.profile.onboarded)
    }
}

class StepsTest {
    @Test fun firstReadingOnlySetsTheStart() {
        val (steps, last) = com.rotitrack.app.domain.Steps.record(emptyMap(), -1, 12_000, "2026-11-11")
        org.junit.Assert.assertEquals(emptyMap<String, Int>(), steps)
        org.junit.Assert.assertEquals(12_000L, last)
    }

    @Test fun readingsAddUpPerDayAndSurviveARestart() {
        val s = com.rotitrack.app.domain.Steps
        var (steps, last) = s.record(emptyMap(), 12_000, 12_500, "2026-11-11")
        org.junit.Assert.assertEquals(500, steps["2026-11-11"])
        s.record(steps, last, 13_000, "2026-11-11").let { steps = it.first; last = it.second }
        org.junit.Assert.assertEquals(1_000, steps["2026-11-11"])
        // Phone restarted: the counter starts again from zero, so 300 are new steps.
        s.record(steps, last, 300, "2026-11-11").let { steps = it.first; last = it.second }
        org.junit.Assert.assertEquals(1_300, steps["2026-11-11"])
        // A new day starts its own count.
        s.record(steps, last, 900, "2026-11-12").let { steps = it.first; last = it.second }
        org.junit.Assert.assertEquals(600, steps["2026-11-12"])
        // A sensor glitch is ignored.
        org.junit.Assert.assertEquals(600, s.record(steps, last, 900 + 60_000, "2026-11-12").first["2026-11-12"])
    }

    @Test fun stepsCountAsCaloriesBurned() {
        org.junit.Assert.assertEquals(40, com.rotitrack.app.domain.Steps.kcal(1_000, 70.0))
        val store = AppStore(object : Storage {
            var t: String? = null
            override fun load() = t
            override fun save(text: String) { t = text }
            override fun clear() { t = null }
        })
        val today = Days.today()
        store.updateProfile { it.copy(weightKg = 70.0) }
        store.recordStepCounter(1_000, today)
        store.recordStepCounter(6_000, today)
        org.junit.Assert.assertEquals(5_000, store.stepsFor(today))
        org.junit.Assert.assertEquals(200, store.burned(today))
    }
}

class SleepTest {
    private fun store() = AppStore(object : Storage {
        var t: String? = null
        override fun load() = t
        override fun save(text: String) { t = text }
        override fun clear() { t = null }
    })

    @Test fun nightCountsForTheDayYouWakeUp() {
        val zone = java.time.ZoneId.of("Asia/Kolkata")
        val (start, end) = com.rotitrack.app.domain.Sleep.night("2026-11-11", 22, 30, 6, 30, zone)
        assertEquals(8 * 60, ((end - start) / 60_000).toInt())
        assertEquals("2026-11-10", com.rotitrack.app.domain.Sleep.dayOf(start, zone))
        assertEquals("2026-11-11", com.rotitrack.app.domain.Sleep.dayOf(end, zone))
        // An afternoon nap stays on the same day.
        val (napStart, napEnd) = com.rotitrack.app.domain.Sleep.night("2026-11-11", 14, 0, 15, 0, zone)
        assertEquals(60, ((napEnd - napStart) / 60_000).toInt())
    }

    @Test fun startAndEndLogsANightAndDropsMisTaps() {
        val s = store()
        val t0 = 1_000_000_000_000L
        s.startSleep(t0)
        s.endSleep(t0 + 7 * 3_600_000L)
        assertEquals(1, s.state.sleep.entries.size)
        assertEquals(null, s.state.sleep.activeStart)
        assertEquals(7 * 60, com.rotitrack.app.domain.Sleep.minutes(s.lastSleep!!))
        s.rateSleep(s.lastSleep!!.id, 3)
        assertEquals(3, s.lastSleep!!.quality)
        // Started by mistake and stopped a minute later: nothing is logged.
        s.startSleep(t0 + 8 * 3_600_000L)
        s.endSleep(t0 + 8 * 3_600_000L + 60_000)
        assertEquals(1, s.state.sleep.entries.size)
        // Manual entries must be between 10 minutes and 16 hours.
        assertTrue(!s.addSleep(t0, t0 + 20 * 3_600_000L))
        assertTrue(s.addSleep(t0 + 30 * 3_600_000L, t0 + 37 * 3_600_000L, 2))
        s.removeSleep(s.lastSleep!!.id)
        assertEquals(1, s.state.sleep.entries.size)
    }
}
