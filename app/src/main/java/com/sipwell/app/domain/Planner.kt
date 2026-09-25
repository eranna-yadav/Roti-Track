package com.sipwell.app.domain

import com.sipwell.app.data.Diet
import com.sipwell.app.data.Food
import com.sipwell.app.data.MealSlot
import com.sipwell.app.data.MealTemplate
import com.sipwell.app.data.Region
import com.sipwell.app.data.TEMPLATES
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class PlannedItem(val food: Food, val servings: Double)

data class PlannedMeal(
    val slot: MealSlot,
    val name: String,
    val target: Int,
    val items: List<PlannedItem>,
) {
    val kcal get() = items.sumOf { it.food.kcal * it.servings }.roundToInt()
    val protein get() = items.sumOf { it.food.protein * it.servings }.roundToInt()
    val carbs get() = items.sumOf { it.food.carbs * it.servings }.roundToInt()
    val fat get() = items.sumOf { it.food.fat * it.servings }.roundToInt()
}

object Planner {
    /** FNV-1a, so a day's plan is stable across launches and devices. */
    private fun hash(s: String): Long {
        var h = 2166136261L
        for (c in s) {
            h = h xor c.code.toLong()
            h = (h * 16777619L) and 0xFFFFFFFFL
        }
        return h
    }

    private fun templateDiet(t: MealTemplate, foodById: (String) -> Food?): Diet =
        t.items.mapNotNull { foodById(it.foodId)?.diet }.maxByOrNull { it.ordinal } ?: Diet.VEG

    fun candidates(slot: MealSlot, diet: Diet, region: Region, foodById: (String) -> Food?): List<MealTemplate> {
        val allowed = TEMPLATES.getValue(slot).filter { templateDiet(it, foodById).ordinal <= diet.ordinal }
        if (region == Region.ALL) return allowed
        val local = allowed.filter { it.region == region || it.region == null }
        return if (local.size >= 2) local else allowed
    }

    private fun roundTo(n: Double, step: Double) = max(step, (n / step).roundToInt() * step)

    /** Scales the template's flexible items so the meal lands near [target] kcal. */
    fun scale(t: MealTemplate, target: Int, foodById: (String) -> Food?): List<PlannedItem> {
        val resolved = t.items.mapNotNull { item -> foodById(item.foodId)?.let { Triple(it, item.servings, item.fixed) } }
        val fixedKcal = resolved.filter { it.third }.sumOf { it.first.kcal * it.second }
        val flexKcal = resolved.filterNot { it.third }.sumOf { it.first.kcal * it.second }
        val factor = if (flexKcal > 0) min(2.0, max(0.5, (target - fixedKcal) / flexKcal)) else 1.0
        return resolved.map { (food, servings, fixed) ->
            PlannedItem(food, if (fixed) servings else roundTo(servings * factor, if (food.piece) 1.0 else 0.5))
        }
    }

    fun planMeal(
        day: String,
        slot: MealSlot,
        shuffle: Int,
        dailyKcal: Int,
        diet: Diet,
        region: Region,
        foodById: (String) -> Food?,
    ): PlannedMeal {
        val target = (dailyKcal * slot.share).roundToInt()
        val pool = candidates(slot, diet, region, foodById)
        val template = pool[((hash("$day:${slot.name}") + shuffle) % pool.size).toInt()]
        return PlannedMeal(slot, template.name, target, scale(template, target, foodById))
    }
}

fun formatServings(n: Double): String = when {
    n == 0.5 -> "½"
    n % 1.0 == 0.0 -> n.toInt().toString()
    (n * 2) % 1.0 == 0.0 -> "${n.toInt()}½"
    else -> "%.1f".format(n)
}
