package com.rotitrack.app.account

import com.rotitrack.app.data.Food
import com.rotitrack.app.data.FoodCategory
import com.rotitrack.app.data.MealSlot
import com.rotitrack.app.data.builtinFood
import com.rotitrack.app.i18n.t

/** One dish the scanner saw. [foodId] is a catalog id, or empty for a dish the app doesn't know. */
data class ScannedItem(
    val foodId: String,
    val name: String,
    val serving: String,
    val servings: Double,
    val kcal: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val confidence: String,
) {
    /**
     * The food to log. Catalog matches use the app's own nutrition values; other dishes use the
     * scanner's estimate, logged as a one-off (not added to "My foods").
     */
    fun toFood(id: String): Food =
        builtinFood(foodId) ?: Food(id, name, "🍽️", FoodCategory.CUSTOM, serving, kcal, protein, carbs, fat)
}

data class ScanResult(val items: List<ScannedItem>, val note: String)

/** Reads a food photo (Pro). */
interface FoodScanner {
    /** Non-null when scanning can't happen here, e.g. without the online setup. */
    val unavailableReason: String?

    /** [jpeg] is a downsized photo of the plate; [meal] helps the scanner guess portions. */
    suspend fun scan(jpeg: ByteArray, meal: MealSlot): ScanResult
}

object NoFoodScanner : FoodScanner {
    override val unavailableReason: String get() = t("Food scanning needs the online (Firebase) setup.")
    override suspend fun scan(jpeg: ByteArray, meal: MealSlot): ScanResult = throw AuthException(unavailableReason)
}

/** Turns the server's answer (a map from the callable function) into a [ScanResult]. */
fun scanResultFrom(data: Map<*, *>?): ScanResult {
    fun num(v: Any?): Double = (v as? Number)?.toDouble() ?: 0.0
    val items = (data?.get("items") as? List<*>).orEmpty().mapNotNull { raw ->
        val m = raw as? Map<*, *> ?: return@mapNotNull null
        ScannedItem(
            foodId = m["foodId"] as? String ?: "",
            name = (m["name"] as? String).orEmpty().ifBlank { "Food" },
            serving = (m["serving"] as? String).orEmpty().ifBlank { "1 serving" },
            servings = num(m["servings"]).coerceIn(0.5, 20.0),
            kcal = num(m["kcal"]).toInt().coerceAtLeast(0),
            protein = num(m["protein"]).coerceAtLeast(0.0),
            carbs = num(m["carbs"]).coerceAtLeast(0.0),
            fat = num(m["fat"]).coerceAtLeast(0.0),
            confidence = m["confidence"] as? String ?: "low",
        )
    }
    return ScanResult(items, data?.get("note") as? String ?: "")
}
