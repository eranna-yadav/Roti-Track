package com.sipwell.app.domain

import com.sipwell.app.data.Gender
import com.sipwell.app.data.Profile
import com.sipwell.app.data.WeightGoal
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Nutrition {
    /** Mifflin–St Jeor resting energy, kcal/day. */
    fun bmr(weightKg: Double, heightCm: Int, age: Int, gender: Gender): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return base + when (gender) {
            Gender.MALE -> 5.0
            Gender.FEMALE -> -161.0
            Gender.OTHER -> -78.0
        }
    }

    fun tdee(p: Profile): Double = bmr(p.weightKg, p.heightCm, p.age, p.gender) * p.activity.factor

    /** Maintenance ± the goal delta, floored at a safe minimum, rounded to 10 kcal. */
    fun recommendedCalories(p: Profile): Int {
        val floor = if (p.gender == Gender.MALE) 1500.0 else 1200.0
        val raw = min(4500.0, max(floor, tdee(p) + p.goal.delta))
        return (raw / 10).roundToInt() * 10
    }

    /** ~35 ml per kg of body weight, nudged by gender, clamped and rounded to 10 ml. */
    fun recommendedWaterMl(weightKg: Double, gender: Gender): Int {
        val perKg = when (gender) {
            Gender.MALE -> 36.0
            Gender.FEMALE -> 33.0
            Gender.OTHER -> 34.5
        }
        return (min(5000.0, max(800.0, weightKg * perKg)) / 10).roundToInt() * 10
    }

    data class Macros(val protein: Int, val carbs: Int, val fat: Int)

    /**
     * Carb-forward split in line with ICMR-NIN guidance for Indian diets, nudged
     * toward protein when losing weight. Protein never drops below 0.8 g/kg.
     */
    fun macroTargets(kcal: Int, weightKg: Double, goal: WeightGoal): Macros {
        val proteinShare = if (goal == WeightGoal.LOSE) 0.25 else 0.20
        val protein = max((weightKg * 0.8).roundToInt(), (kcal * proteinShare / 4).roundToInt())
        val fat = (kcal * 0.25 / 9).roundToInt()
        val carbs = max(0, ((kcal - protein * 4 - fat * 9) / 4.0).roundToInt())
        return Macros(protein, carbs, fat)
    }

    fun bmi(weightKg: Double, heightCm: Int): Double {
        val m = heightCm / 100.0
        return if (m > 0) weightKg / (m * m) else 0.0
    }

    /** Asian-Indian BMI cut-offs, which sit lower than the WHO defaults. */
    fun bmiLabel(value: Double): String = when {
        value < 18.5 -> "Underweight"
        value < 23 -> "Healthy"
        value < 25 -> "Overweight"
        else -> "Obese"
    }
}
