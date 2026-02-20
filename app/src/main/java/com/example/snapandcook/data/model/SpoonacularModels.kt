package com.example.snapandcook.data.model

import com.google.gson.annotations.SerializedName

// ─── Spoonacular: findByIngredients ──────────────────────────────────────────

/**
 * A lightweight recipe summary returned by /recipes/findByIngredients.
 */
data class RecipeSummary(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val imageUrl: String?,
    @SerializedName("usedIngredientCount") val usedIngredientCount: Int,
    @SerializedName("missedIngredientCount") val missedIngredientCount: Int
)

// ─── Spoonacular: /recipes/{id}/information ──────────────────────────────────

/**
 * Full recipe details from /recipes/{id}/information.
 */
data class RecipeDetail(
    @SerializedName("id") val id: Int,
    @SerializedName("title") val title: String,
    @SerializedName("image") val imageUrl: String?,
    @SerializedName("readyInMinutes") val readyInMinutes: Int,
    @SerializedName("servings") val servings: Int,
    @SerializedName("nutrition") val nutrition: Nutrition?,
    @SerializedName("extendedIngredients") val extendedIngredients: List<ExtendedIngredient>?,
    @SerializedName("analyzedInstructions") val analyzedInstructions: List<AnalyzedInstruction>?,
    @SerializedName("summary") val summary: String?
)

data class Nutrition(
    @SerializedName("nutrients") val nutrients: List<Nutrient>?
)

data class Nutrient(
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("unit") val unit: String
)

data class ExtendedIngredient(
    @SerializedName("id") val id: Int,
    @SerializedName("original") val original: String,
    @SerializedName("name") val name: String,
    @SerializedName("amount") val amount: Double,
    @SerializedName("unit") val unit: String,
    @SerializedName("image") val image: String?
)

data class AnalyzedInstruction(
    @SerializedName("name") val name: String,
    @SerializedName("steps") val steps: List<InstructionStep>?
)

data class InstructionStep(
    @SerializedName("number") val number: Int,
    @SerializedName("step") val step: String,
    @SerializedName("equipment") val equipment: List<Equipment>?
)

data class Equipment(
    @SerializedName("name") val name: String
)
