package com.example.snapandcook.util

import com.example.snapandcook.data.local.SavedRecipe
import com.example.snapandcook.data.model.*
import org.junit.Assert.*
import org.junit.Test

class RecipeConverterTest {

    @Test
    fun toSavedRecipe_convertsDetailToSavedRecipe() {
        val detail = RecipeDetail(
            id = 123,
            title = "Test Recipe",
            imageUrl = "https://example.com/image.jpg",
            readyInMinutes = 30,
            servings = 4,
            nutrition = Nutrition(
                nutrients = listOf(
                    Nutrient(name = "Calories", amount = 500.0, unit = "kcal")
                )
            ),
            extendedIngredients = listOf(
                ExtendedIngredient(1, "2 cups flour", "flour", 2.0, "cups", null),
                ExtendedIngredient(2, "1 egg", "egg", 1.0, "piece", null)
            ),
            analyzedInstructions = listOf(
                AnalyzedInstruction(
                    name = "Instructions",
                    steps = listOf(
                        InstructionStep(1, "Mix flour and egg", null)
                    )
                )
            ),
            summary = null
        )

        val result = RecipeConverter.toSavedRecipe(detail)

        assertEquals(123, result.recipeId)
        assertEquals("Test Recipe", result.title)
        assertEquals("https://example.com/image.jpg", result.imageUrl)
        assertEquals(30, result.readyInMinutes)
        assertEquals(4, result.servings)
        assertEquals(500, result.calories)
    }

    @Test
    fun toSavedRecipe_handlesNullIngredients() {
        val detail = RecipeDetail(
            id = 1, title = "Recipe", imageUrl = null, readyInMinutes = 10,
            servings = 2, nutrition = null, extendedIngredients = null,
            analyzedInstructions = null, summary = null
        )

        val result = RecipeConverter.toSavedRecipe(detail)

        assertTrue(result.ingredientsJson.isNotEmpty())
        assertTrue(result.stepsJson.isNotEmpty())
    }

    @Test
    fun parseIngredients_parsesValidJson() {
        val json = "[\"flour\", \"egg\", \"butter\"]"

        val result = RecipeConverter.parseIngredients(json)

        assertEquals(3, result.size)
        assertEquals("flour", result[0])
        assertEquals("egg", result[1])
    }

    @Test
    fun parseIngredients_returnsEmptyListForInvalidJson() {
        val json = "invalid json"

        val result = RecipeConverter.parseIngredients(json)

        assertTrue(result.isEmpty())
    }

    @Test
    fun parseSteps_parsesValidJson() {
        val json = "[\"Step 1\", \"Step 2\", \"Step 3\"]"

        val result = RecipeConverter.parseSteps(json)

        assertEquals(3, result.size)
        assertEquals("Step 1", result[0])
    }

    @Test
    fun parseSteps_returnsEmptyListForEmptyJson() {
        val json = "[]"

        val result = RecipeConverter.parseSteps(json)

        assertTrue(result.isEmpty())
    }

    @Test
    fun toSavedRecipe_extractsCaloriesCorrectly() {
        val detail = RecipeDetail(
            id = 1, title = "Test", imageUrl = null, readyInMinutes = 20,
            servings = 2,
            nutrition = Nutrition(
                nutrients = listOf(
                    Nutrient("Protein", 20.0, "g"),
                    Nutrient("Calories", 350.5, "kcal"),
                    Nutrient("Fat", 10.0, "g")
                )
            ),
            extendedIngredients = null, analyzedInstructions = null, summary = null
        )

        val result = RecipeConverter.toSavedRecipe(detail)

        assertEquals(350, result.calories)
    }
}
