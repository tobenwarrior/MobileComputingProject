package com.example.snapandcook.data.remote

import com.example.snapandcook.data.model.RecipeDetail
import com.example.snapandcook.data.model.RecipeSummary
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for the Spoonacular Recipe API.
 *
 * Docs: https://spoonacular.com/food-api/docs
 * Base URL: https://api.spoonacular.com/
 */
interface SpoonacularApi {

    /**
     * Find recipes by a comma-separated list of ingredients.
     *
     * @param ingredients  Comma-separated ingredient names (e.g. "eggs,cheese,tomatoes")
     * @param number       Max number of results to return (default 5)
     * @param ranking      1 = maximise used ingredients, 2 = minimise missing ingredients
     * @param ignorePantry Ignore pantry staples like water, salt, etc.
     * @param apiKey       Spoonacular API key
     */
    @GET("recipes/findByIngredients")
    suspend fun findByIngredients(
        @Query("ingredients") ingredients: String,
        @Query("number") number: Int = 5,
        @Query("ranking") ranking: Int = 1,
        @Query("ignorePantry") ignorePantry: Boolean = true,
        @Query("apiKey") apiKey: String
    ): Response<List<RecipeSummary>>

    /**
     * Get full details for a specific recipe, including nutrition and step instructions.
     *
     * @param id             Spoonacular recipe ID
     * @param includeNutrition Whether to include nutritional info
     * @param apiKey         Spoonacular API key
     */
    @GET("recipes/{id}/information")
    suspend fun getRecipeDetail(
        @Path("id") id: Int,
        @Query("includeNutrition") includeNutrition: Boolean = true,
        @Query("apiKey") apiKey: String
    ): Response<RecipeDetail>
}
