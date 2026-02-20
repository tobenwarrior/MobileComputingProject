package com.example.snapandcook.ui.saved

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import com.example.snapandcook.data.local.AppDatabase
import com.example.snapandcook.data.local.SavedRecipe
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

/**
 * ViewModel for the Saved Recipes screen.
 *
 * Exposes a reactive list of saved recipes, with optional search filtering.
 */
class SavedRecipesViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).recipeDao()

    // Current search query (empty = show all)
    private val _query = MutableLiveData("")
    val query: LiveData<String> = _query

    // Switches between "all" and "search" queries based on the current query string
    val recipes: LiveData<List<SavedRecipe>> = _query.switchMap { q ->
        if (q.isBlank()) dao.getAllRecipes() else dao.searchRecipes(q)
    }

    /** Update the search query. */
    fun setQuery(text: String) {
        _query.value = text
    }

    /** Delete a saved recipe from the database. */
    fun deleteRecipe(recipe: SavedRecipe) {
        viewModelScope.launch {
            dao.deleteRecipe(recipe)
        }
    }
}
