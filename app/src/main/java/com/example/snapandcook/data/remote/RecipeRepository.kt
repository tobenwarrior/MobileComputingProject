package com.example.snapandcook.data.remote

import com.example.snapandcook.data.model.RecipeDetail
import com.example.snapandcook.data.model.RecipeSummary
import retrofit2.Response

/**
 * Repository that mediates between the ViewModel and the Spoonacular network layer.
 *
 * All methods are suspend functions and must be called from a coroutine scope.
 * API key rotation is handled transparently via [withKeyRotation]: when a key
 * returns HTTP 402 (daily quota exhausted), it is marked as used up and the
 * next available key is tried automatically.
 */
class RecipeRepository {

    private val api = RetrofitClient.api

    // ── Key-rotation helper ───────────────────────────────────────────────────

    /**
     * Executes [apiCall] with the current Spoonacular API key.
     * If the response is HTTP 402, the key is marked exhausted and the call is
     * retried with the next available key. This continues until a non-402 response
     * is received, or all keys are exhausted.
     *
     * @throws Exception with a user-readable message when all keys are exhausted.
     */
    private suspend fun <T> withKeyRotation(apiCall: suspend (key: String) -> Response<T>): Response<T> {
        while (SpoonacularKeyManager.hasAvailableKey) {
            val key = SpoonacularKeyManager.currentKey ?: break
            val response = apiCall(key)
            if (response.code() == 402) {
                // This key's daily quota is used up — close the error body to free
                // the connection and try the next key.
                response.errorBody()?.close()
                SpoonacularKeyManager.markExhausted(key)
            } else {
                return response
            }
        }
        throw Exception(
            "All ${SpoonacularKeyManager.totalKeys} Spoonacular API key(s) have hit " +
            "their daily limit. Please try again tomorrow."
        )
    }

    // ── Public methods ────────────────────────────────────────────────────────

    /**
     * Searches Spoonacular for recipes matching the given ingredients.
     * Automatically rotates API keys on quota errors.
     *
     * @param ingredients List of ingredient name strings.
     * @return A [Result] wrapping a list of [RecipeSummary], or an error.
     */
    suspend fun findRecipes(ingredients: List<String>): Result<List<RecipeSummary>> {
        return try {
            // Cap at 15 ingredients — too many causes poor Spoonacular matching
            val query = ingredients.take(15).joinToString(",") { it.trim() }
            val response = withKeyRotation { key ->
                api.findByIngredients(
                    ingredients = query,
                    number = 6,
                    ranking = 2, // 2 = minimise missing ingredients (better relevance)
                    apiKey = key
                )
            }
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
     * Automatically rotates API keys on quota errors.
     *
     * @param recipeId The Spoonacular recipe ID.
     * @return A [Result] wrapping a [RecipeDetail], or an error.
     */
    suspend fun getRecipeDetail(recipeId: Int): Result<RecipeDetail> {
        return try {
            val response = withKeyRotation { key ->
                api.getRecipeDetail(
                    id = recipeId,
                    includeNutrition = true,
                    apiKey = key
                )
            }
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

    /**
     * Searches for a YouTube video matching the recipe title.
     * Returns the YouTube video ID, or null if nothing found.
     * Fails silently — a missing video is not a UI error.
     */
    suspend fun searchRecipeVideo(title: String): String? {
        return try {
            val response = withKeyRotation { key ->
                api.searchVideos(query = title, number = 1, apiKey = key)
            }
            if (response.isSuccessful) {
                response.body()?.videos?.firstOrNull()?.youTubeId
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
