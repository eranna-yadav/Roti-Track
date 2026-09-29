package com.rotitrack.app

import com.rotitrack.app.account.scanResultFrom
import com.rotitrack.app.data.FoodCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FoodScannerTest {
    @Test fun scanAnswersBecomeFoodsToLog() {
        val result = scanResultFrom(
            mapOf(
                "items" to listOf(
                    mapOf("foodId" to "roti", "name" to "Roti", "serving" to "1 roti", "servings" to 3, "kcal" to 999,
                        "protein" to 1, "carbs" to 1, "fat" to 1, "confidence" to "high"),
                    mapOf("foodId" to "", "name" to "Aloo Tikki", "serving" to "1 piece", "servings" to 2.0, "kcal" to 150,
                        "protein" to 3.5, "carbs" to 20, "fat" to 7, "confidence" to "low"),
                    "not a map",
                ),
                "note" to "Two items",
            ),
        )
        assertEquals(2, result.items.size)
        assertEquals("Two items", result.note)

        // A catalog match logs with the app's own nutrition, not the scanner's estimate.
        val roti = result.items[0].toFood("scan-1")
        assertEquals("roti", roti.id)
        assertTrue(roti.kcal < 999)
        assertEquals(3.0, result.items[0].servings, 0.0)

        // Anything else is a one-off with the scanner's estimate.
        val tikki = result.items[1].toFood("scan-2")
        assertEquals("scan-2", tikki.id)
        assertEquals("Aloo Tikki", tikki.name)
        assertEquals(150, tikki.kcal)
        assertEquals(FoodCategory.CUSTOM, tikki.category)

        assertEquals(0, scanResultFrom(null).items.size)
    }
}
