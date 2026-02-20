package com.example.snapandcook.ui.verification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snapandcook.R
import com.example.snapandcook.databinding.ActivityVerificationBinding
import com.example.snapandcook.ui.recipe.RecipeResultActivity
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show
import com.example.snapandcook.util.toast

/**
 * Step 2 — Verification screen.
 *
 * Displays the ML Kit–detected ingredient list. The user can:
 *  - Delete wrong items by tapping the red trash icon.
 *  - Manually add missing items via the text field + "Add" button.
 *  - Confirm the final list to trigger recipe generation.
 */
class VerificationActivity : AppCompatActivity() {

    private lateinit var binding: ActivityVerificationBinding
    private val viewModel: VerificationViewModel by viewModels()
    private lateinit var adapter: IngredientAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecycler()
        setupListeners()
        observeViewModel()

        // Defer analysis until after onStart so LiveData observers are active
        val uriStrings = intent.getStringArrayListExtra(EXTRA_URIS) ?: emptyList<String>()
        val uris = uriStrings.map { Uri.parse(it) }
        if (uris.isNotEmpty()) {
            window.decorView.post {
                viewModel.analyzeImages(uris)
            }
        }
    }

    // ── RecyclerView ──────────────────────────────────────────────────────────

    private fun setupRecycler() {
        adapter = IngredientAdapter { index ->
            viewModel.removeIngredient(index)
        }
        binding.rvIngredients.apply {
            layoutManager = LinearLayoutManager(this@VerificationActivity)
            adapter = this@VerificationActivity.adapter
        }
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnAdd.setOnClickListener { addManualIngredient() }

        binding.etAdd.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addManualIngredient()
                true
            } else false
        }

        binding.btnConfirm.setOnClickListener {
            val names = viewModel.getIngredientNames()
            if (names.isEmpty()) {
                toast(getString(R.string.verification_empty))
            } else {
                RecipeResultActivity.start(this, names)
            }
        }
    }

    private fun addManualIngredient() {
        val text = binding.etAdd.text?.toString() ?: return
        if (text.isNotBlank()) {
            viewModel.addManualIngredient(text)
            binding.etAdd.text?.clear()
            // Hide keyboard
            val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            imm.hideSoftInputFromWindow(binding.etAdd.windowToken, 0)
        }
    }

    // ── Observer ──────────────────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.isAnalyzing.observe(this) { analyzing ->
            if (analyzing) binding.layoutAnalyzing.show() else binding.layoutAnalyzing.gone()
        }

        viewModel.ingredients.observe(this) { list ->
            adapter.submitList(list.toList())
            val count = list.size
            binding.tvCount.text = getString(R.string.verification_count, count)
            if (count == 0 && viewModel.isAnalyzing.value == false) {
                binding.tvEmpty.show()
                binding.rvIngredients.gone()
            } else {
                binding.tvEmpty.gone()
                binding.rvIngredients.show()
            }
        }

        viewModel.error.observe(this) { msg ->
            if (!msg.isNullOrBlank()) toast(msg)
        }
    }

    companion object {
        private const val EXTRA_URIS = "extra_uris"

        fun start(context: Context, uriStrings: List<String>) {
            context.startActivity(
                Intent(context, VerificationActivity::class.java).apply {
                    putStringArrayListExtra(EXTRA_URIS, ArrayList(uriStrings))
                }
            )
        }
    }
}
