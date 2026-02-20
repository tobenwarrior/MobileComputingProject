package com.example.snapandcook.ml

import android.graphics.Bitmap
import com.example.snapandcook.data.model.Ingredient
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabel
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * ML Kit–powered ingredient detector.
 *
 * Uses Google ML Kit's on-device [ImageLabeling] to identify food-related items
 * in a [Bitmap] captured from the camera or loaded from the gallery.
 *
 * The detector runs entirely on-device — no network call is required for this step.
 *
 * @param confidenceThreshold Minimum ML Kit confidence score (0–1) required to
 *                            accept a label as a detected ingredient. Defaults to 0.65.
 */
class IngredientDetector(
    private val confidenceThreshold: Float = 0.65f
) {

    /**
     * A curated set of common food-related keywords that ML Kit might return.
     * Labels not containing any of these keywords are filtered out.
     */
    private val foodKeywords = setOf(
        // Produce
        "apple", "banana", "orange", "lemon", "lime", "grape", "strawberry",
        "blueberry", "raspberry", "cherry", "peach", "pear", "mango", "pineapple",
        "watermelon", "melon", "tomato", "potato", "onion", "garlic", "carrot",
        "broccoli", "cauliflower", "cabbage", "lettuce", "spinach", "kale",
        "pepper", "cucumber", "zucchini", "eggplant", "corn", "avocado",
        "mushroom", "celery", "ginger", "beet", "radish", "asparagus", "pea",
        "bean", "pumpkin", "squash", "artichoke", "leek", "shallot",
        // Proteins
        "chicken", "beef", "pork", "lamb", "turkey", "fish", "salmon",
        "tuna", "shrimp", "crab", "lobster", "egg", "tofu", "tempeh",
        // Dairy
        "milk", "cheese", "butter", "cream", "yogurt", "ice cream",
        // Grains & Pantry
        "bread", "rice", "pasta", "flour", "sugar", "salt", "oil", "vinegar",
        "sauce", "soy sauce", "honey", "jam", "cereal", "oat", "noodle",
        // Herbs & Spices
        "herb", "basil", "parsley", "cilantro", "oregano", "thyme", "rosemary",
        "mint", "dill", "cinnamon", "paprika", "cumin", "turmeric",
        // General food terms
        "food", "vegetable", "fruit", "meat", "seafood", "dairy", "ingredient",
        "produce", "grain", "legume", "nut", "almond", "walnut", "cashew",
        "peanut", "coconut", "olive"
    )

    /**
     * Analyses a [Bitmap] and returns a list of detected [Ingredient]s.
     *
     * The function is a suspend function and should be called from a coroutine.
     *
     * @param bitmap The image to analyse.
     * @return A deduplicated list of [Ingredient]s ordered by confidence (highest first).
     */
    suspend fun detectIngredients(bitmap: Bitmap): List<Ingredient> {
        val image = InputImage.fromBitmap(bitmap, 0)

        val options = ImageLabelerOptions.Builder()
            .setConfidenceThreshold(confidenceThreshold)
            .build()

        val labeler = ImageLabeling.getClient(options)

        return try {
            val labels: List<ImageLabel> = suspendCancellableCoroutine { cont ->
                labeler.process(image)
                    .addOnSuccessListener { result -> cont.resume(result) }
                    .addOnFailureListener { e -> cont.resumeWithException(e) }
            }

            labels
                .filter { label: ImageLabel ->
                    val lower = label.text.lowercase()
                    foodKeywords.any { keyword -> lower.contains(keyword) }
                }
                .distinctBy { label: ImageLabel -> label.text.lowercase() }
                .sortedByDescending { label: ImageLabel -> label.confidence }
                .map { label: ImageLabel ->
                    Ingredient(
                        name = label.text.replaceFirstChar { c -> c.uppercaseChar() },
                        confidence = label.confidence,
                        isManual = false
                    )
                }
        } catch (e: Exception) {
            emptyList()
        } finally {
            labeler.close()
        }
    }

    /**
     * Analyses multiple bitmaps and merges the results into a single deduplicated list.
     *
     * @param bitmaps List of bitmaps to analyse.
     * @return Merged, deduplicated list of [Ingredient]s.
     */
    suspend fun detectFromMultipleBitmaps(bitmaps: List<Bitmap>): List<Ingredient> {
        val allIngredients = mutableListOf<Ingredient>()
        bitmaps.forEach { bitmap ->
            allIngredients.addAll(detectIngredients(bitmap))
        }
        // Deduplicate by name (case-insensitive), keep the highest confidence entry
        return allIngredients
            .groupBy { it.name.lowercase() }
            .map { (_, group) -> group.maxByOrNull { it.confidence ?: 0f }!! }
            .sortedByDescending { it.confidence }
    }
}
