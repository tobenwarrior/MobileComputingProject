package com.example.snapandcook.data.local

import androidx.lifecycle.LiveData
import androidx.room.*

/**
 * Data Access Object for saved recipes.
 *
 * All queries are observed via LiveData so the UI updates automatically.
 */
@Dao
interface RecipeDao {

    /** Insert or replace a recipe. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: SavedRecipe)

    /** Delete a specific recipe. */
    @Delete
    suspend fun deleteRecipe(recipe: SavedRecipe)

    /** Delete a recipe by its Spoonacular ID. */
    @Query("DELETE FROM saved_recipes WHERE recipeId = :recipeId")
    suspend fun deleteRecipeById(recipeId: Int)

    /** Get all saved recipes ordered by save date (newest first). */
    @Query("SELECT * FROM saved_recipes ORDER BY savedAt DESC")
    fun getAllRecipes(): LiveData<List<SavedRecipe>>

    /** Search saved recipes by title. */
    @Query("SELECT * FROM saved_recipes WHERE title LIKE '%' || :query || '%' ORDER BY savedAt DESC")
    fun searchRecipes(query: String): LiveData<List<SavedRecipe>>

    /** Check if a recipe is already saved. */
    @Query("SELECT COUNT(*) FROM saved_recipes WHERE recipeId = :recipeId")
    suspend fun isRecipeSaved(recipeId: Int): Int

    /** Get a single recipe by ID. */
    @Query("SELECT * FROM saved_recipes WHERE recipeId = :recipeId LIMIT 1")
    suspend fun getRecipeById(recipeId: Int): SavedRecipe?

    /** Get the three most recently saved recipes (for the home screen preview). */
    @Query("SELECT * FROM saved_recipes ORDER BY savedAt DESC LIMIT 3")
    fun getRecentRecipes(): LiveData<List<SavedRecipe>>
}
