package com.example.snapandcook.ui.recipe

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.snapandcook.data.local.AppDatabase
import com.example.snapandcook.data.local.SavedRecipe
import com.example.snapandcook.data.model.RecipeDetail
import com.example.snapandcook.data.model.RecipeSummary
import com.example.snapandcook.data.remote.RecipeRepository
import com.example.snapandcook.util.RecipeConverter
import kotlinx.coroutines.launch

/**
 * ViewModel for the Recipe Result screen.
 *
 * Handles fetching recipe suggestions and full recipe details from Spoonacular,
 * and saving/un-saving recipes to the local Room database.
 */
class RecipeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = RecipeRepository()
    private val dao = AppDatabase.getInstance(application).recipeDao()

    // List of recipe summaries returned by findByIngredients
    private val _recipeSummaries = MutableLiveData<List<RecipeSummary>>()
    val recipeSummaries: LiveData<List<RecipeSummary>> = _recipeSummaries

    // Full detail of the currently displayed recipe
    private val _recipeDetail = MutableLiveData<RecipeDetail?>()
    val recipeDetail: LiveData<RecipeDetail?> = _recipeDetail

    // Whether this recipe is already saved
    private val _isSaved = MutableLiveData(false)
    val isSaved: LiveData<Boolean> = _isSaved

    // YouTube video ID for the current recipe (null = no video found)
    private val _videoId = MutableLiveData<String?>(null)
    val videoId: LiveData<String?> = _videoId

    // Loading / error state
    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    // Index of the currently selected recipe in _recipeSummaries
    private var currentIndex = 0

    /**
     * Search Spoonacular for recipes that match the given ingredient list,
     * then automatically load the first result's full detail.
     */
    fun findRecipes(ingredients: List<String>) {
        _isLoading.value = true
        _error.value = null
        currentIndex = 0

        viewModelScope.launch {
            val result = repository.findRecipes(ingredients)
            result.onSuccess { summaries ->
                _recipeSummaries.value = summaries
                if (summaries.isNotEmpty()) {
                    loadDetail(summaries[0].id)
                } else {
                    _isLoading.value = false
                    _error.value = "No recipes found for your ingredients. Try adding more!"
                }
            }.onFailure { e ->
                _isLoading.value = false
                _error.value = e.localizedMessage ?: "Failed to fetch recipes."
            }
        }
    }

    /**
     * Load the full detail for a recipe by its Spoonacular ID and check if it is saved.
     */
    fun loadDetail(recipeId: Int) {
        _isLoading.value = true
        _error.value = null
        _videoId.value = null

        viewModelScope.launch {
            val result = repository.getRecipeDetail(recipeId)
            result.onSuccess { detail ->
                _recipeDetail.value = detail
                _isSaved.value = dao.isRecipeSaved(recipeId) > 0
                // Fetch video in parallel — non-blocking, fails silently
                launch {
                    _videoId.value = repository.searchRecipeVideo(detail.title)
                }
            }.onFailure { e ->
                _error.value = e.localizedMessage ?: "Failed to load recipe details."
            }
            _isLoading.value = false
        }
    }

    /** Load the next recipe suggestion in the list. */
    fun loadNextRecipe() {
        val summaries = _recipeSummaries.value ?: return
        if (currentIndex < summaries.size - 1) {
            currentIndex++
            loadDetail(summaries[currentIndex].id)
        }
    }

    /** Load the previous recipe suggestion. */
    fun loadPreviousRecipe() {
        val summaries = _recipeSummaries.value ?: return
        if (currentIndex > 0) {
            currentIndex--
            loadDetail(summaries[currentIndex].id)
        }
    }

    val currentRecipeIndex get() = currentIndex
    val totalRecipes get() = _recipeSummaries.value?.size ?: 0

    /** Save the currently displayed recipe to Room. */
    fun saveCurrentRecipe() {
        val detail = _recipeDetail.value ?: return
        viewModelScope.launch {
            val saved = RecipeConverter.toSavedRecipe(detail)
            dao.insertRecipe(saved)
            _isSaved.value = true
        }
    }

    /** Delete the currently displayed recipe from Room. */
    fun unsaveCurrentRecipe() {
        val detail = _recipeDetail.value ?: return
        viewModelScope.launch {
            dao.deleteRecipeById(detail.id)
            _isSaved.value = false
        }
    }

    /** Build a flat list of step strings from the current recipe detail. */
    fun getSteps(): List<String> {
        return _recipeDetail.value
            ?.analyzedInstructions
            ?.flatMap { it.steps ?: emptyList() }
            ?.map { it.step }
            ?: emptyList()
    }

    /** Build a list of equipment names per step (parallel to getSteps()). */
    fun getEquipment(): List<List<String>> {
        return _recipeDetail.value
            ?.analyzedInstructions
            ?.flatMap { it.steps ?: emptyList() }
            ?.map { step -> step.equipment?.map { it.name } ?: emptyList() }
            ?: emptyList()
    }

    /** Build a SavedRecipe from the current detail (used to pass to CookingMode). */
    fun getCurrentSavedRecipe(): SavedRecipe? {
        val detail = _recipeDetail.value ?: return null
        return RecipeConverter.toSavedRecipe(detail)
    }
}
