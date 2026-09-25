package com.sipwell.app.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.sipwell.app.data.AppState
import com.sipwell.app.data.Diet
import com.sipwell.app.data.FOODS
import com.sipwell.app.data.Food
import com.sipwell.app.data.FoodCategory
import com.sipwell.app.data.MealEntry
import com.sipwell.app.data.MealSlot
import com.sipwell.app.data.Profile
import com.sipwell.app.data.WaterEntry
import com.sipwell.app.data.builtinFood
import com.sipwell.app.data.drinkById
import com.sipwell.app.domain.Days
import com.sipwell.app.domain.Nutrition
import com.sipwell.app.domain.PlannedMeal
import com.sipwell.app.domain.Planner
import kotlinx.serialization.json.Json
import java.util.UUID
import kotlin.math.roundToInt

/** Where the state is persisted. Android writes a file; tests use memory. */
interface Storage {
    fun load(): String?
    fun save(text: String)
    fun clear()
}

data class Totals(val kcal: Int = 0, val protein: Double = 0.0, val carbs: Double = 0.0, val fat: Double = 0.0)

/**
 * The single source of truth. Every mutation goes through [update], which
 * re-derives the automatic goals and persists the result.
 */
class AppStore(private val storage: Storage) {

    var state: AppState by mutableStateOf(decode(storage.load()))
        private set

    val profile: Profile get() = state.profile

    private fun update(transform: (AppState) -> AppState) {
        state = withDerivedGoals(transform(state))
        storage.save(json.encodeToString(AppState.serializer(), state))
    }

    // ---------------------------------------------------------------- profile

    fun updateProfile(transform: (Profile) -> Profile) = update { it.copy(profile = transform(it.profile)) }

    fun resetAll() {
        storage.clear()
        state = withDerivedGoals(AppState())
    }

    // ------------------------------------------------------------------ water

    fun addWater(ml: Int, drinkId: String = "water") = update {
        it.copy(water = it.water + WaterEntry(uid(), Days.today(), System.currentTimeMillis(), ml, drinkId))
    }

    fun removeWater(id: String) = update { s -> s.copy(water = s.water.filterNot { it.id == id }) }

    fun waterForDay(day: String): List<WaterEntry> = state.water.filter { it.day == day }.sortedBy { it.ts }

    /** Hydration-weighted ml: a cup of chai counts for less than its volume. */
    fun effectiveMl(e: WaterEntry): Int = (e.ml * drinkById(e.drinkId).hydration).roundToInt()

    fun waterTotal(day: String): Int = state.water.filter { it.day == day }.sumOf { effectiveMl(it) }

    /** Consecutive days with any water logged, ending today (or yesterday if today is empty). */
    fun streak(): Int {
        val days = state.water.map { it.day }.toSet()
        var day = Days.today()
        if (day !in days) day = Days.shift(day, -1)
        var n = 0
        while (day in days) {
            n++
            day = Days.shift(day, -1)
        }
        return n
    }

    // ------------------------------------------------------------------ foods

    fun foodById(id: String): Food? = builtinFood(id) ?: state.customFoods.firstOrNull { it.id == id }

    val allFoods: List<Food> get() = state.customFoods + FOODS

    fun recentFoods(limit: Int = 10): List<Food> =
        state.meals.asReversed().asSequence().map { it.foodId }.distinct().mapNotNull { foodById(it) }.take(limit).toList()

    fun addCustomFood(name: String, serving: String, kcal: Int, protein: Double, carbs: Double, fat: Double, diet: Diet): Food {
        val food = Food("custom-${uid()}", name, "🍽️", FoodCategory.CUSTOM, serving, kcal, protein, carbs, fat, diet)
        update { it.copy(customFoods = listOf(food) + it.customFoods) }
        return food
    }

    fun removeCustomFood(id: String) = update { s -> s.copy(customFoods = s.customFoods.filterNot { it.id == id }) }

    // ------------------------------------------------------------------ meals

    fun logFood(food: Food, servings: Double, slot: MealSlot, day: String, planKey: String? = null) =
        update { it.copy(meals = it.meals + entry(food, servings, slot, day, planKey)) }

    fun logPlannedMeal(day: String, meal: PlannedMeal) {
        val key = planKey(day, meal.slot)
        update { s -> s.copy(meals = s.meals + meal.items.map { entry(it.food, it.servings, meal.slot, day, key) }) }
    }

    fun removeMeal(id: String) = update { s -> s.copy(meals = s.meals.filterNot { it.id == id }) }

    fun mealsForDay(day: String): List<MealEntry> = state.meals.filter { it.day == day }.sortedBy { it.ts }

    fun totals(day: String): Totals = state.meals.filter { it.day == day }.fold(Totals()) { t, m ->
        Totals(t.kcal + m.kcal, t.protein + m.protein, t.carbs + m.carbs, t.fat + m.fat)
    }

    fun macroTargets(): Nutrition.Macros = Nutrition.macroTargets(profile.calorieGoal, profile.weightKg, profile.goal)

    // ------------------------------------------------------------------- plan

    fun plan(day: String): List<PlannedMeal> = MealSlot.entries.map { slot ->
        Planner.planMeal(
            day, slot, state.planShuffles[planKey(day, slot)] ?: 0,
            profile.calorieGoal, profile.diet, profile.region, ::foodById,
        )
    }

    fun swapMeal(day: String, slot: MealSlot) = update {
        val k = planKey(day, slot)
        it.copy(planShuffles = it.planShuffles + (k to (it.planShuffles[k] ?: 0) + 1))
    }

    fun isPlanLogged(day: String, slot: MealSlot): Boolean {
        val k = planKey(day, slot)
        return state.meals.any { it.planKey == k }
    }

    companion object {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

        fun decode(text: String?): AppState = withDerivedGoals(
            text?.let { runCatching { json.decodeFromString(AppState.serializer(), it) }.getOrNull() } ?: AppState()
        )

        fun withDerivedGoals(s: AppState): AppState {
            var p = s.profile
            if (!p.calorieGoalCustom) p = p.copy(calorieGoal = Nutrition.recommendedCalories(p))
            if (!p.waterGoalCustom) p = p.copy(waterGoalMl = Nutrition.recommendedWaterMl(p.weightKg, p.gender))
            return if (p == s.profile) s else s.copy(profile = p)
        }

        private fun planKey(day: String, slot: MealSlot) = "$day|${slot.name}"

        private fun uid() = UUID.randomUUID().toString()

        private fun entry(food: Food, servings: Double, slot: MealSlot, day: String, planKey: String?) = MealEntry(
            id = uid(),
            day = day,
            ts = System.currentTimeMillis(),
            slot = slot,
            foodId = food.id,
            name = food.name,
            emoji = food.emoji,
            serving = food.serving,
            servings = servings,
            kcal = (food.kcal * servings).roundToInt(),
            protein = round1(food.protein * servings),
            carbs = round1(food.carbs * servings),
            fat = round1(food.fat * servings),
            planKey = planKey,
        )

        private fun round1(v: Double) = (v * 10).roundToInt() / 10.0
    }
}
