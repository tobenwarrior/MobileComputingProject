package com.example.snapandcook.ui.saved

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snapandcook.R
import com.example.snapandcook.data.local.SavedRecipe
import com.example.snapandcook.databinding.ActivitySavedRecipesBinding
import com.example.snapandcook.ui.cooking.CookingModeActivity
import com.example.snapandcook.util.RecipeConverter
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show

/**
 * Saved Recipes screen.
 *
 * Displays the user's offline recipe collection from Room with a live search bar.
 * Tapping a recipe launches Cooking Mode; the delete icon removes it from the database.
 */
class SavedRecipesActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySavedRecipesBinding
    private val viewModel: SavedRecipesViewModel by viewModels()
    private lateinit var adapter: SavedRecipeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySavedRecipesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupSearch()
        setupListeners()
        observeViewModel()
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecycler() {
        adapter = SavedRecipeAdapter(
            onItemClick = { recipe -> openCookingMode(recipe) },
            onDelete    = { recipe -> confirmDelete(recipe) }
        )
        binding.rvSaved.apply {
            layoutManager = LinearLayoutManager(this@SavedRecipesActivity)
            adapter = this@SavedRecipesActivity.adapter
        }
    }

    // ── Search ────────────────────────────────────────────────────────────────

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                viewModel.setQuery(s?.toString() ?: "")
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }
    }

    // ── ViewModel observation ─────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.recipes.observe(this) { list ->
            adapter.submitList(list)
            if (list.isEmpty()) {
                binding.layoutEmpty.show()
                binding.rvSaved.gone()
            } else {
                binding.layoutEmpty.gone()
                binding.rvSaved.show()
            }
        }
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private fun confirmDelete(recipe: SavedRecipe) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.saved_delete_confirm))
            .setPositiveButton(getString(R.string.saved_delete_btn)) { _, _ ->
                viewModel.deleteRecipe(recipe)
            }
            .setNegativeButton(getString(R.string.saved_cancel_btn), null)
            .show()
    }

    private fun openCookingMode(recipe: SavedRecipe) {
        val steps = RecipeConverter.parseSteps(recipe.stepsJson)
        if (steps.isEmpty()) return
        CookingModeActivity.start(this, recipe.title, steps)
    }
}
