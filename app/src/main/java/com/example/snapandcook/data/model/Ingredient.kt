package com.example.snapandcook.data.model

/**
 * Represents a detected or manually entered ingredient.
 *
 * @param name The display name of the ingredient.
 * @param confidence Detection confidence score (0–1). Null for Gemini-detected or manually entered items.
 * @param isManual True if the user typed this ingredient manually.
 */
data class Ingredient(
    val name: String,
    val confidence: Float? = null,
    val isManual: Boolean = false
)
