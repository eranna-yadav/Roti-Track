package com.rotitrack.app

import com.rotitrack.app.data.Diet
import com.rotitrack.app.data.FOODS
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
import com.rotitrack.app.store.AppStore
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
        assertEquals(2310, restored.profile.waterGoalMl)
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
