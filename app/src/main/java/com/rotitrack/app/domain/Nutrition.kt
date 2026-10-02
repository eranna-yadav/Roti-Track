package com.rotitrack.app.domain

import com.rotitrack.app.i18n.t

import com.rotitrack.app.data.Activity
import com.rotitrack.app.data.Gender
import com.rotitrack.app.data.Profile
import com.rotitrack.app.data.WeightGoal
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

    /**
     * Water to drink each day: 40 ml/kg for men, 35 for women, plus 250 ml when moderately
     * active and 500 ml when very active. That is close to the ~3 L (men) / ~2.2 L (women)
     * of drinks behind the usual 3.7 L / 2.7 L total-fluid advice, since about a fifth of
     * fluid comes from food. Clamped and rounded to 10 ml.
     */
    fun recommendedWaterMl(weightKg: Double, gender: Gender, activity: Activity = Activity.LIGHT): Int {
        val perKg = when (gender) {
            Gender.MALE -> 40.0
            Gender.FEMALE -> 35.0
            Gender.OTHER -> 37.5
        }
        val extra = when (activity) {
            Activity.SEDENTARY, Activity.LIGHT -> 0
            Activity.MODERATE -> 250
            Activity.ACTIVE, Activity.ATHLETE -> 500
        }
        return (min(5000.0, max(800.0, weightKg * perKg + extra)) / 10).roundToInt() * 10
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
        value < 18.5 -> t("Underweight")
        value < 23 -> t("Healthy")
        value < 25 -> t("Overweight")
        else -> t("Obese")
    }
}
