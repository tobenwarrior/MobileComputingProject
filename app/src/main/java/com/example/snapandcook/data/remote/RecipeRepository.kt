package com.example.snapandcook.data.remote

import com.example.snapandcook.BuildConfig
import com.example.snapandcook.data.model.RecipeDetail
import com.example.snapandcook.data.model.RecipeSummary

/**
 * Repository that mediates between the ViewModel and the Spoonacular network layer.
 *
 * All methods are suspend functions and must be called from a coroutine scope.
 */
class RecipeRepository {

    private val api = RetrofitClient.api
    private val apiKey = BuildConfig.SPOONACULAR_API_KEY

    /**
     * Searches Spoonacular for recipes matching the given ingredients.
     *
     * @param ingredients List of ingredient name strings.
     * @return A [Result] wrapping a list of [RecipeSummary], or an error.
     */
    suspend fun findRecipes(ingredients: List<String>): Result<List<RecipeSummary>> {
        return try {
            val query = ingredients.joinToString(",") { it.trim() }
            val response = api.findByIngredients(
                ingredients = query,
                number = 6,
                apiKey = apiKey
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (!body.isNullOrEmpty()) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("No recipes found for your ingredients."))
                }
            } else {
                Result.failure(Exception("API error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Fetches the full details for a specific recipe by its Spoonacular ID.
     *
     * @param recipeId The Spoonacular recipe ID.
     * @return A [Result] wrapping a [RecipeDetail], or an error.
     */
    suspend fun getRecipeDetail(recipeId: Int): Result<RecipeDetail> {
        return try {
            val response = api.getRecipeDetail(
                id = recipeId,
                includeNutrition = true,
                apiKey = apiKey
            )
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.success(body)
                } else {
                    Result.failure(Exception("Empty response from server."))
                }
            } else {
                Result.failure(Exception("API error ${response.code()}: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
