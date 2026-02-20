package com.example.snapandcook.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a recipe saved offline by the user.
 *
 * @param recipeId Spoonacular recipe ID (used as primary key).
 * @param title Recipe title.
 * @param imageUrl URL of the recipe image.
 * @param readyInMinutes Cooking time.
 * @param servings Number of servings.
 * @param calories Calories per serving (may be null if unavailable).
 * @param ingredientsJson JSON-serialized list of ingredient strings.
 * @param stepsJson JSON-serialized list of instruction step strings.
 * @param savedAt Unix timestamp when the recipe was saved.
 */
@Entity(tableName = "saved_recipes")
data class SavedRecipe(
    @PrimaryKey val recipeId: Int,
    val title: String,
    val imageUrl: String?,
    val readyInMinutes: Int,
    val servings: Int,
    val calories: Int?,
    val ingredientsJson: String,  // JSON array of ingredient strings
    val stepsJson: String,         // JSON array of step strings
    val savedAt: Long = System.currentTimeMillis()
)
