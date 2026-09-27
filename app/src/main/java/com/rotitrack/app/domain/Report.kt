package com.rotitrack.app.domain

import com.rotitrack.app.data.ExerciseEntry
import com.rotitrack.app.data.MealEntry
import com.rotitrack.app.data.WeightEntry
import com.rotitrack.app.store.AppStore
import kotlin.math.roundToInt

data class ReportDay(
    val day: String,
    val kcal: Int,
    val goal: Int,
    val protein: Int,
    val carbs: Int,
    val fat: Int,
    val burned: Int,
    val waterMl: Int,
)

/** Everything the PDF summary shows, gathered from the store for a date range. */
data class Report(
    val name: String,
    val from: String,
    val to: String,
    val profileLines: List<String>,
    val days: List<ReportDay>,
    val meals: List<MealEntry>,
    val exercises: List<ExerciseEntry>,
    val weights: List<WeightEntry>,
    /** Monday of each week → average weight that week. */
    val weeklyWeights: List<Pair<String, Double>>,
) {
    private val logged get() = days.filter { it.kcal > 0 }
    val avgKcal: Int get() = logged.map { it.kcal }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
    val avgProtein: Int get() = logged.map { it.protein }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
    val avgCarbs: Int get() = logged.map { it.carbs }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
    val avgFat: Int get() = logged.map { it.fat }.average().takeIf { !it.isNaN() }?.roundToInt() ?: 0
    val totalBurned: Int get() = exercises.sumOf { it.kcal }
    val daysWithinGoal: Int get() = logged.count { it.kcal <= it.goal }
    val daysLogged: Int get() = logged.size
}

object Reports {
    fun build(store: AppStore, name: String, days: Int, today: String = Days.today()): Report {
        val range = Days.lastDays(today, days)
        val from = range.first()
        val s = store.state
        val p = s.profile
        val m = store.macroTargets()
        return Report(
            name = name,
            from = from,
            to = today,
            profileLines = listOf(
                "${p.age} years · ${p.heightCm} cm · ${fmt1(p.weightKg)} kg · BMI ${fmt1(Nutrition.bmi(p.weightKg, p.heightCm))}",
                "Goal: ${p.goal.label} · ${p.activity.label} · ${p.diet.name.lowercase().replaceFirstChar { it.uppercase() }}",
                "Daily targets: ${p.calorieGoal} kcal · P ${m.protein} g · C ${m.carbs} g · F ${m.fat} g · water ${p.waterGoalMl} ml",
            ),
            days = range.map { d ->
                val t = store.totals(d)
                ReportDay(d, t.kcal, store.calorieGoalFor(d), t.protein.roundToInt(), t.carbs.roundToInt(), t.fat.roundToInt(), store.burned(d), store.waterTotal(d))
            },
            meals = s.meals.filter { it.day in from..today }.sortedWith(compareBy({ it.day }, { it.slot.ordinal }, { it.ts })),
            exercises = s.exercises.filter { it.day in from..today }.sortedBy { it.ts },
            weights = s.weights.filter { it.day in from..today },
            weeklyWeights = s.weights.filter { it.day in from..today }
                .groupBy { Days.parse(it.day).let { d -> d.minusDays((d.dayOfWeek.value - 1).toLong()).toString() } }
                .map { (week, list) -> week to (list.map { it.kg }.average() * 10).roundToInt() / 10.0 }
                .sortedBy { it.first },
        )
    }

    private fun fmt1(v: Double) = ((v * 10).roundToInt() / 10.0).let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() }
}
