package com.rotitrack.app

import com.rotitrack.app.data.RecipeCategory
import com.rotitrack.app.data.Recipes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipesTest {
    @Test fun everyRecipeIsComplete() {
        val all = Recipes.all
        assertEquals(125, all.size)
        RecipeCategory.entries.forEach { c -> assertTrue("no ${c.key}", all.any { it.category == c }) }
        assertEquals(all.size, all.map { it.id }.toSet().size)
        for (r in all) {
            assertTrue("${r.title}: kcal", r.kcal in 0..800)
            assertTrue("${r.title}: ingredients", r.ingredients.size >= 3)
            assertTrue("${r.title}: steps", r.steps.size >= 3)
            assertTrue("${r.title}: benefits", r.benefits.isNotEmpty())
            assertTrue("${r.title}: intro", r.intro.length > 40)
            r.steps.forEach { assertTrue("${r.title}: step '$it'", it.first().isUpperCase()) }
        }
    }

    @Test fun loggingARecipeKeepsItsCaloriesAndProtein() {
        val r = Recipes.byId("r001")!!
        val food = r.toFood()
        assertEquals(r.kcal, food.kcal)
        assertEquals(r.protein.toDouble(), food.protein, 0.0)
        // Estimated carbs and fat roughly make up the rest of the calories.
        val total = food.protein * 4 + food.carbs * 4 + food.fat * 9
        assertTrue("total $total vs ${r.kcal}", kotlin.math.abs(total - r.kcal) < 5)
    }

    @Test fun searchAndCategories() {
        assertTrue(Recipes.search("paneer", null).isNotEmpty())
        assertTrue(Recipes.search("", RecipeCategory.NONVEG).all { it.category == RecipeCategory.NONVEG })
        assertEquals("🥞", Recipes.emojiFor("Besan Cheela with Veggies", RecipeCategory.VEG))   // not an egg
    }
}
