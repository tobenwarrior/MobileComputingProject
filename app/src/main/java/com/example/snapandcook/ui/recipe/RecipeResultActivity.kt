package com.example.snapandcook.ui.recipe

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.bumptech.glide.Glide
import com.example.snapandcook.R
import com.example.snapandcook.databinding.ActivityRecipeResultBinding
import com.example.snapandcook.ui.cooking.CookingModeActivity
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.formatMinutes
import com.example.snapandcook.util.show
import android.util.Log
import com.example.snapandcook.util.toast

/**
 * Step 3 — Recipe Result screen.
 *
 * Displays the recipe image, title, meta-info, ingredients list, and step-by-step
 * instructions fetched from Spoonacular. The user can:
 *  - Page through multiple recipe suggestions (Prev / Next).
 *  - Save the recipe to Room for offline access.
 *  - Launch Cooking Mode to cook hands-free.
 */
class RecipeResultActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRecipeResultBinding
    private val viewModel: RecipeViewModel by viewModels()
    private var ingredients: List<String> = emptyList()
    private var fromSavedId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRecipeResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val baseBottomPadding = binding.bottomBar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            binding.bottomBar.updatePadding(bottom = baseBottomPadding + navInsets.bottom)
            windowInsets
        }

        fromSavedId = intent.getIntExtra(EXTRA_RECIPE_ID, -1)
        ingredients = intent.getStringArrayListExtra(EXTRA_INGREDIENTS) ?: emptyList()
        Log.d("RecipeResult", "=== INGREDIENTS RECEIVED: $ingredients ===")

        setupListeners()
        observeViewModel()

        if (fromSavedId != -1) {
            // Launched from Saved Recipes — load single recipe by ID, no pagination
            binding.layoutPagination.gone()
            viewModel.loadDetail(fromSavedId)
        } else {
            viewModel.findRecipes(ingredients)
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.ivRecipe.setOnClickListener {
            val url = viewModel.recipeDetail.value?.imageUrl ?: return@setOnClickListener
            showFullscreenImage(url)
        }

        binding.btnRetry.setOnClickListener {
            viewModel.findRecipes(ingredients)
        }

        binding.btnPrevRecipe.setOnClickListener { viewModel.loadPreviousRecipe() }
        binding.btnNextRecipe.setOnClickListener { viewModel.loadNextRecipe() }

        binding.btnSave.setOnClickListener {
            if (viewModel.isSaved.value == true) {
                viewModel.unsaveCurrentRecipe()
                toast(getString(R.string.recipe_already_saved))
            } else {
                viewModel.saveCurrentRecipe()
                toast(getString(R.string.recipe_saved_success))
            }
        }

        binding.btnCook.setOnClickListener {
            val steps = viewModel.getSteps()
            if (steps.isEmpty()) {
                toast("No instructions available for this recipe.")
                return@setOnClickListener
            }
            val title = viewModel.recipeDetail.value?.title ?: ""
            val equipment = viewModel.getEquipment()
            CookingModeActivity.start(this, title, steps, equipment)
        }
    }

    // ── ViewModel observation ─────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.isLoading.observe(this) { loading ->
            if (loading) {
                binding.layoutLoading.show()
                binding.contentScroll.gone()
                binding.layoutError.gone()
            } else {
                binding.layoutLoading.gone()
            }
        }

        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrBlank()) {
                binding.layoutError.show()
                binding.contentScroll.gone()
                binding.tvError.text = msg
            }
        }

        viewModel.recipeDetail.observe(this) { detail ->
            detail ?: return@observe
            binding.contentScroll.show()
            binding.layoutError.gone()

            // Hero image
            Glide.with(this)
                .load(detail.imageUrl)
                .placeholder(R.drawable.ic_placeholder_food)
                .error(R.drawable.ic_placeholder_food)
                .centerCrop()
                .into(binding.ivRecipe)

            // Title
            binding.tvTitle.text = detail.title

            // Meta tags
            binding.tvTime.text = "⏱ ${detail.readyInMinutes.formatMinutes()}"
            binding.tvServings.text = "🍽 Serves ${detail.servings}"
            val calories = detail.nutrition?.nutrients
                ?.firstOrNull { it.name.equals("Calories", ignoreCase = true) }
                ?.amount?.toInt()
            if (calories != null) {
                binding.tvCalories.text = "🔥 $calories kcal"
                binding.tvCalories.show()
            }

            // Pagination label — hidden when launched from saved recipe
            if (fromSavedId == -1) {
                val idx = viewModel.currentRecipeIndex + 1
                val total = viewModel.totalRecipes
                binding.tvRecipePage.text = "$idx / $total"
                binding.btnPrevRecipe.isEnabled = idx > 1
                binding.btnNextRecipe.isEnabled = idx < total
            }

            // Ingredients — green ✓ for items the user already has, bullet for need-to-buy
            val userOwned = ingredients.map { it.lowercase().trim() }
            val greenColor = ContextCompat.getColor(this, R.color.brand_secondary_dark)
            val spanned = SpannableStringBuilder()
            val recipeIngredients = detail.extendedIngredients ?: emptyList()
            recipeIngredients.forEachIndexed { i, ingredient ->
                val name = ingredient.name.lowercase().trim()
                val hasIt = userOwned.isNotEmpty() &&
                    userOwned.any { u -> u.contains(name) || name.contains(u) }
                val lineStart = spanned.length
                spanned.append(if (hasIt) "✓ " else "• ")
                spanned.append(ingredient.original)
                if (i < recipeIngredients.size - 1) spanned.append("\n")
                if (hasIt) {
                    spanned.setSpan(
                        ForegroundColorSpan(greenColor),
                        lineStart, spanned.length,
                        Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
            }
            binding.tvIngredients.text = if (spanned.isEmpty()) "—" else spanned

            // Steps — dynamically inflated rows for large, readable text
            binding.llSteps.removeAllViews()
            val steps = detail.analyzedInstructions
                ?.flatMap { it.steps ?: emptyList() } ?: emptyList()

            steps.forEachIndexed { i, step ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = android.view.Gravity.TOP
                    setPadding(0, 0, 0, 20)
                }

                // Step number circle
                val numView = TextView(this).apply {
                    text = "${i + 1}"
                    textSize = 14f
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(ContextCompat.getColor(this@RecipeResultActivity, R.color.text_on_brand))
                    background = ContextCompat.getDrawable(this@RecipeResultActivity, R.drawable.bg_step_indicator)
                    gravity = Gravity.CENTER
                    val size = resources.getDimensionPixelSize(R.dimen.step_circle_size)
                    layoutParams = LinearLayout.LayoutParams(size, size).also {
                        it.marginEnd = resources.getDimensionPixelSize(R.dimen.step_circle_margin)
                        it.topMargin = 2
                    }
                    setPadding(0, 0, 0, 0)
                }

                // Step text
                val stepText = TextView(this).apply {
                    text = step.step
                    textSize = 16f
                    setTextColor(ContextCompat.getColor(this@RecipeResultActivity, R.color.text_primary))
                    setLineSpacing(4f, 1f)
                    layoutParams = LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f
                    )
                }

                row.addView(numView)
                row.addView(stepText)
                binding.llSteps.addView(row)
            }
        }

        viewModel.isSaved.observe(this) { saved ->
            binding.btnSave.text = if (saved) "✓ Saved" else getString(R.string.recipe_btn_save)
        }

        viewModel.videoId.observe(this) { videoId ->
            if (videoId != null) {
                binding.layoutVideoSection.show()
                val thumbUrl = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
                Glide.with(this)
                    .load(thumbUrl)
                    .centerCrop()
                    .into(binding.ivVideoThumb)
                binding.cvVideo.setOnClickListener {
                    startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=$videoId"))
                    )
                }
            } else {
                binding.layoutVideoSection.gone()
            }
        }
    }

    private fun showFullscreenImage(url: String) {
        val dialog = Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        val imageView = ImageView(this).apply {
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.ic_placeholder_food)
            .into(imageView)
        dialog.setContentView(imageView)
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    companion object {
        private const val EXTRA_INGREDIENTS = "extra_ingredients"
        private const val EXTRA_RECIPE_ID = "extra_recipe_id"

        fun start(context: Context, ingredients: List<String>) {
            context.startActivity(
                Intent(context, RecipeResultActivity::class.java).apply {
                    putStringArrayListExtra(EXTRA_INGREDIENTS, ArrayList(ingredients))
                }
            )
        }

        fun startFromSaved(context: Context, recipeId: Int) {
            context.startActivity(
                Intent(context, RecipeResultActivity::class.java).apply {
                    putExtra(EXTRA_RECIPE_ID, recipeId)
                }
            )
        }
    }
}
