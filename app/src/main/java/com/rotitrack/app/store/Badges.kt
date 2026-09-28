package com.rotitrack.app.store

import com.rotitrack.app.data.AppState
import com.rotitrack.app.i18n.t
import com.rotitrack.app.domain.Days

data class Badge(val id: String, val emoji: String, private val titleEn: String, private val descriptionEn: String) {
    val title: String get() = t(titleEn)
    val description: String get() = t(descriptionEn)
}

/** Every badge, in display order, with the rule that unlocks it. */
object Badges {
    val ALL = listOf(
        Badge("first_meal", "🍽️", "First bite", "Log your first meal"),
        Badge("first_water", "💧", "First sip", "Log your first glass of water"),
        Badge("water_goal", "🥤", "Hydrated", "Reach your water goal for a day"),
        Badge("calorie_goal", "🎯", "On target", "Finish a day within your calorie goal"),
        Badge("streak_3", "🔥", "3-day streak", "Log something 3 days in a row"),
        Badge("streak_7", "🌟", "7-day streak", "Log something 7 days in a row"),
        Badge("streak_30", "🏆", "30-day streak", "Log something 30 days in a row"),
        Badge("meals_50", "🍛", "Thali master", "Log 50 meals"),
        Badge("first_exercise", "🏃", "On the move", "Log your first exercise"),
        Badge("first_fast", "⏳", "First fast", "Complete an intermittent fast"),
        Badge("weight_logged", "⚖️", "Weigh-in", "Log your weight twice"),
    )

    fun byId(id: String) = ALL.firstOrNull { it.id == id }

    /** Ids of every badge the state has earned (whether or not already recorded). */
    fun earned(s: AppState, streak: Int, waterTotal: (String) -> Int): Set<String> {
        val out = mutableSetOf<String>()
        if (s.meals.isNotEmpty()) out += "first_meal"
        if (s.water.isNotEmpty()) out += "first_water"
        if (s.water.map { it.day }.distinct().any { waterTotal(it) >= s.profile.waterGoalMl }) out += "water_goal"
        val today = Days.today()
        val pastMealDays = s.meals.map { it.day }.distinct().filter { it < today }
        if (pastMealDays.any { d -> s.meals.filter { it.day == d }.sumOf { it.kcal } in 1..s.profile.calorieGoal }) out += "calorie_goal"
        if (streak >= 3) out += "streak_3"
        if (streak >= 7) out += "streak_7"
        if (streak >= 30) out += "streak_30"
        if (s.meals.size >= 50) out += "meals_50"
        if (s.exercises.isNotEmpty()) out += "first_exercise"
        if (s.fasting.history.any { it.end - it.start >= it.targetHours * 3_600_000L }) out += "first_fast"
        if (s.weights.size >= 2) out += "weight_logged"
        return out
    }
}
