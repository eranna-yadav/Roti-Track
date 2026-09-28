package com.rotitrack.app.store

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.rotitrack.app.data.ExerciseType
import com.rotitrack.app.data.AppState
import com.rotitrack.app.data.ExerciseEntry
import com.rotitrack.app.data.FastRecord
import com.rotitrack.app.data.Prefs
import com.rotitrack.app.data.WeightEntry
import com.rotitrack.app.data.burnedKcal
import com.rotitrack.app.data.Diet
import com.rotitrack.app.data.FOODS
import com.rotitrack.app.data.Food
import com.rotitrack.app.data.FoodCategory
import com.rotitrack.app.data.MealEntry
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.WaterEntry
import com.rotitrack.app.data.builtinFood
import com.rotitrack.app.data.drinkById
import com.rotitrack.app.domain.Days
import com.rotitrack.app.domain.Nutrition
import com.rotitrack.app.domain.PlannedMeal
import com.rotitrack.app.domain.Planner
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
    val prefs: Prefs get() = state.prefs

    /** Badges unlocked since the UI last showed a celebration. */
    val celebrations = mutableStateListOf<Badge>()

    init {
        // Record badges earned before this version, without celebrating them.
        val earned = Badges.earned(state, streak(), ::waterTotal) - state.badges.keys
        if (earned.isNotEmpty()) {
            state = state.copy(badges = state.badges + earned.associateWith { System.currentTimeMillis() })
        }
    }

    private fun update(transform: (AppState) -> AppState) {
        state = withDerivedGoals(transform(state))
        val fresh = Badges.earned(state, streak(), ::waterTotal) - state.badges.keys
        if (fresh.isNotEmpty()) {
            state = state.copy(badges = state.badges + fresh.associateWith { System.currentTimeMillis() })
            if (state.prefs.badgeCelebrations) celebrations += fresh.mapNotNull { Badges.byId(it) }
        }
        storage.save(json.encodeToString(AppState.serializer(), state))
    }

    fun updatePrefs(transform: (Prefs) -> Prefs) = update { it.copy(prefs = transform(it.prefs)) }

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

    /** Consecutive days with any water or food logged, ending today (or yesterday if today is empty). */
    fun streak(): Int {
        val days = state.water.map { it.day }.toSet() + state.meals.map { it.day }
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

    /** Leftover calories from the day before [day], capped at 200, when that day had food logged. */
    fun rollover(day: String): Int {
        val prev = Days.shift(day, -1)
        if (state.meals.none { it.day == prev }) return 0
        return (profile.calorieGoal - totals(prev).kcal).coerceIn(0, 200)
    }

    /** The day's calorie target after the burned-calories and rollover preferences. */
    fun calorieGoalFor(day: String): Int =
        profile.calorieGoal +
            (if (prefs.addBurnedCalories) burned(day) else 0) +
            (if (prefs.rolloverCalories) rollover(day) else 0)

    // --------------------------------------------------------------- exercise

    fun addExercise(activity: ExerciseType, minutes: Int, day: String) = update {
        val kcal = burnedKcal(activity, it.profile.weightKg, minutes)
        it.copy(exercises = it.exercises + ExerciseEntry(uid(), day, System.currentTimeMillis(), activity.id, activity.nameEn, activity.emoji, minutes, kcal))
    }

    fun removeExercise(id: String) = update { s -> s.copy(exercises = s.exercises.filterNot { it.id == id }) }

    fun exercisesForDay(day: String): List<ExerciseEntry> = state.exercises.filter { it.day == day }.sortedBy { it.ts }

    fun burned(day: String): Int = state.exercises.filter { it.day == day }.sumOf { it.kcal }

    // ----------------------------------------------------------------- weight

    /** Sets the current weight and records it for today (one entry per day). */
    fun logWeight(kg: Double) = update { s ->
        val today = Days.today()
        s.copy(
            profile = s.profile.copy(weightKg = kg),
            weights = (s.weights.filterNot { it.day == today } + WeightEntry(today, kg)).sortedBy { it.day },
        )
    }

    // ---------------------------------------------------------------- fasting

    fun setFastTarget(hours: Int) = update { it.copy(fasting = it.fasting.copy(targetHours = hours)) }

    fun startFast(at: Long = System.currentTimeMillis()) = update { it.copy(fasting = it.fasting.copy(activeStart = at)) }

    fun endFast(at: Long = System.currentTimeMillis()) = update { s ->
        val start = s.fasting.activeStart ?: return@update s
        s.copy(fasting = s.fasting.copy(activeStart = null, history = s.fasting.history + FastRecord(start, at, s.fasting.targetHours)))
    }


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
