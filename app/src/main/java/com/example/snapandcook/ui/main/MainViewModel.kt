package com.example.snapandcook.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.snapandcook.data.local.AppDatabase

/**
 * ViewModel for the Home screen.
 *
 * Provides the three most recent saved recipes for the "Recently Saved" preview strip.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val dao = AppDatabase.getInstance(application).recipeDao()

    /** LiveData of the 3 most recently saved recipes. */
    val recentRecipes = dao.getRecentRecipes()
}
