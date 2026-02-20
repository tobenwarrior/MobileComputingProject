package com.example.snapandcook.ml

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.snapandcook.BuildConfig
import com.example.snapandcook.data.model.GeminiContent
import com.example.snapandcook.data.model.GeminiInlineData
import com.example.snapandcook.data.model.GeminiPart
import com.example.snapandcook.data.model.GeminiRequest
import com.example.snapandcook.data.model.Ingredient
import com.example.snapandcook.data.remote.GeminiClient
import java.io.ByteArrayOutputStream

private const val TAG = "IngredientDetector"

private const val PROMPT = """Look at this image and identify every specific food ingredient visible.
Return ONLY a comma-separated list of ingredient names (example: egg, carrot, butter, salt).
Rules:
- Use specific names only — no generic terms like 'food', 'vegetable', 'produce', or 'ingredient'
- One word or short phrase per item (e.g. 'soy sauce', 'bell pepper')
- If no food ingredients are visible, respond with exactly: none"""

/**
 * Ingredient detector powered by Google Gemini 1.5 Flash.
 *
 * Sends each [Bitmap] to Gemini along with a prompt that instructs the model
 * to return a comma-separated list of visible food ingredients. This is far
 * more accurate than generic image-label classifiers because the model
 * understands food context and returns specific, actionable ingredient names.
 */
class IngredientDetector {

    /**
     * Analyses a [Bitmap] and returns a list of detected [Ingredient]s.
     */
    suspend fun detectIngredients(bitmap: Bitmap): List<Ingredient> {
        val base64 = encodeToBase64(bitmap)

        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = PROMPT),
                        GeminiPart(
                            inlineData = GeminiInlineData(
                                mimeType = "image/jpeg",
                                data = base64
                            )
                        )
                    )
                )
            )
        )

        return try {
            val response = GeminiClient.api.generateContent(
                apiKey = BuildConfig.GEMINI_API_KEY,
                request = request
            )

            val text = response.candidates
                ?.firstOrNull()
                ?.content
                ?.parts
                ?.firstOrNull()
                ?.text
                ?.trim()
                ?: return emptyList()

            Log.d(TAG, "Gemini raw response: $text")

            if (text.equals("none", ignoreCase = true)) return emptyList()

            text.split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.equals("none", ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .map { name ->
                    Ingredient(
                        name = name.replaceFirstChar { c -> c.uppercaseChar() },
                        confidence = null,
                        isManual = false
                    )
                }
        } catch (e: retrofit2.HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            Log.e(TAG, "Gemini HTTP ${e.code()} error: $errorBody", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Gemini call failed: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * Analyses multiple bitmaps and merges results into a single deduplicated list.
     */
    suspend fun detectFromMultipleBitmaps(bitmaps: List<Bitmap>): List<Ingredient> {
        val allIngredients = mutableListOf<Ingredient>()
        bitmaps.forEach { bitmap ->
            allIngredients.addAll(detectIngredients(bitmap))
        }
        return allIngredients
            .groupBy { it.name.lowercase() }
            .map { (_, group) -> group.first() }
    }

    private fun encodeToBase64(bitmap: Bitmap): String {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)
    }
}
