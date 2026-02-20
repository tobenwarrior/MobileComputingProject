package com.example.snapandcook.ui.verification

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.snapandcook.data.model.Ingredient
import com.example.snapandcook.ml.IngredientDetector
import com.example.snapandcook.util.decodeBitmapFromUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel for the Verification screen.
 *
 * Manages the list of detected/manual ingredients and the ML Kit detection process.
 */
class VerificationViewModel(application: Application) : AndroidViewModel(application) {

    // Ingredient list (observable by the UI)
    private val _ingredients = MutableLiveData<MutableList<Ingredient>>(mutableListOf())
    val ingredients: LiveData<MutableList<Ingredient>> = _ingredients

    // Loading state for the ML Kit analysis
    private val _isAnalyzing = MutableLiveData(false)
    val isAnalyzing: LiveData<Boolean> = _isAnalyzing

    // Error state
    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private val detector = IngredientDetector()

    /**
     * Decode images from the provided URIs and run ML Kit detection.
     *
     * @param uris List of content URIs pointing to images.
     */
    fun analyzeImages(uris: List<Uri>) {
        _isAnalyzing.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val bitmaps = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        getApplication<Application>().decodeBitmapFromUri(uri)
                    }
                }

                val detected = withContext(Dispatchers.Default) {
                    detector.detectFromMultipleBitmaps(bitmaps)
                }

                val current = _ingredients.value ?: mutableListOf()
                // Merge: don't duplicate names already in the list
                val existing = current.map { it.name.lowercase() }.toSet()
                val newOnes = detected.filter { it.name.lowercase() !in existing }
                current.addAll(newOnes)
                _ingredients.value = current
            } catch (e: Exception) {
                _error.value = "Detection failed: ${e.localizedMessage}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /**
     * Analyse a single [Bitmap] (from CameraX capture).
     */
    fun analyzeBitmap(bitmap: Bitmap) {
        _isAnalyzing.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val detected = withContext(Dispatchers.Default) {
                    detector.detectIngredients(bitmap)
                }
                val current = _ingredients.value ?: mutableListOf()
                val existing = current.map { it.name.lowercase() }.toSet()
                val newOnes = detected.filter { it.name.lowercase() !in existing }
                current.addAll(newOnes)
                _ingredients.value = current
            } catch (e: Exception) {
                _error.value = "Detection failed: ${e.localizedMessage}"
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    /** Add a manually typed ingredient. */
    fun addManualIngredient(name: String) {
        if (name.isBlank()) return
        val trimmed = name.trim().replaceFirstChar { it.uppercaseChar() }
        val current = _ingredients.value ?: mutableListOf()
        if (current.none { it.name.equals(trimmed, ignoreCase = true) }) {
            current.add(Ingredient(name = trimmed, isManual = true))
            _ingredients.value = current
        }
    }

    /** Remove an ingredient at the specified list index. */
    fun removeIngredient(index: Int) {
        val current = _ingredients.value ?: return
        if (index in current.indices) {
            current.removeAt(index)
            _ingredients.value = current
        }
    }

    /** Clear all ingredients. */
    fun clearIngredients() {
        _ingredients.value = mutableListOf()
    }

    /** Return the current ingredient names as a simple list of strings. */
    fun getIngredientNames(): List<String> =
        _ingredients.value?.map { it.name } ?: emptyList()
}
