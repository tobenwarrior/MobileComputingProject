package com.example.snapandcook.ui.verification

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snapandcook.R
import com.example.snapandcook.databinding.ActivityVerificationBinding
import com.example.snapandcook.ui.recipe.RecipeResultActivity
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show
import com.example.snapandcook.util.toast
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning

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

        val baseMarginBottom = (binding.btnConfirm.layoutParams as android.view.ViewGroup.MarginLayoutParams).bottomMargin
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, windowInsets ->
            val navInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())
            val params = binding.btnConfirm.layoutParams as android.view.ViewGroup.MarginLayoutParams
            params.bottomMargin = baseMarginBottom + navInsets.bottom
            binding.btnConfirm.layoutParams = params
            windowInsets
        }

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

        binding.btnScanBarcode.setOnClickListener { startBarcodeScan() }

        binding.btnConfirm.setOnClickListener {
            val names = viewModel.getIngredientNames()
            Log.d("VerificationActivity", "Find Recipes pressed with ingredients: $names")
            if (names.isEmpty()) {
                toast(getString(R.string.verification_empty))
            } else {
                RecipeResultActivity.start(this, names)
            }
        }
    }

    private fun startBarcodeScan() {
        val scanner = GmsBarcodeScanning.getClient(this)
        scanner.startScan()
            .addOnSuccessListener { barcode ->
                val rawValue = barcode.rawValue
                if (!rawValue.isNullOrBlank()) {
                    viewModel.lookupBarcode(rawValue)
                }
            }
            .addOnFailureListener { e ->
                Log.e("VerificationActivity", "Barcode scan failed", e)
                toast(getString(R.string.barcode_scan_failed))
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

        viewModel.barcodeResult.observe(this) { result ->
            if (result == null) return@observe
            when (result) {
                is VerificationViewModel.BarcodeResult.Found ->
                    toast(getString(R.string.barcode_product_added, result.productName))
                is VerificationViewModel.BarcodeResult.NotFound ->
                    toast(getString(R.string.barcode_product_not_found))
                is VerificationViewModel.BarcodeResult.Error ->
                    toast(getString(R.string.error_generic))
            }
            viewModel.clearBarcodeResult()
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
