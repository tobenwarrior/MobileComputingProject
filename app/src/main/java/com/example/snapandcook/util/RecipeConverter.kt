package com.example.snapandcook.util

import com.example.snapandcook.data.local.SavedRecipe
import com.example.snapandcook.data.model.RecipeDetail
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

/**
 * Helper object for converting between [RecipeDetail] (network) and [SavedRecipe] (Room).
 */
object RecipeConverter {

    private val gson = Gson()

    /** Convert a [RecipeDetail] from Spoonacular into a [SavedRecipe] for Room storage. */
    fun toSavedRecipe(detail: RecipeDetail): SavedRecipe {
        val ingredients = detail.extendedIngredients?.map { it.original } ?: emptyList()
        val steps = detail.analyzedInstructions
            ?.flatMap { instruction -> instruction.steps ?: emptyList() }
            ?.map { step -> step.step }
            ?: emptyList()

        val calories = detail.nutrition?.nutrients
            ?.firstOrNull { it.name.equals("Calories", ignoreCase = true) }
            ?.amount?.toInt()

        return SavedRecipe(
            recipeId = detail.id,
            title = detail.title,
            imageUrl = detail.imageUrl,
            readyInMinutes = detail.readyInMinutes,
            servings = detail.servings,
            calories = calories,
            ingredientsJson = gson.toJson(ingredients),
            stepsJson = gson.toJson(steps)
        )
    }

    /** Deserialise ingredient list from JSON stored in Room. */
    fun parseIngredients(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }

    /** Deserialise step list from JSON stored in Room. */
    fun parseSteps(json: String): List<String> {
        val type = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(json, type) ?: emptyList()
    }
}
