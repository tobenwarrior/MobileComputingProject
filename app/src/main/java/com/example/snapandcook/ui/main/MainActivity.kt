package com.example.snapandcook.ui.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.snapandcook.databinding.ActivityMainBinding
import com.example.snapandcook.ui.camera.CameraActivity
import com.example.snapandcook.ui.saved.SavedRecipesActivity
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show
import com.example.snapandcook.util.toast
import java.util.Calendar

/**
 * Home screen — the app entry point after the splash.
 *
 * Presents two primary CTAs (Camera / Gallery), a quick link to saved recipes,
 * and a "Recently Saved" strip showing the last three saved recipes.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var recentAdapter: RecentRecipeAdapter

    // ── Gallery picker ────────────────────────────────────────────────────────
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            CameraActivity.startWithGalleryUris(this, uris.map { it.toString() })
        }
    }

    // ── Permission request (camera) ───────────────────────────────────────────
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) launchCamera()
        else toast("Camera permission is required to take photos.")
    }

    // ── Storage permission (Android < 13) ─────────────────────────────────────
    private val storagePermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) galleryLauncher.launch("image/*")
        else toast("Storage permission is required to pick photos.")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setGreeting()
        setupRecentRecycles()
        setupClickListeners()
        observeViewModel()
    }

    // ── Greeting ─────────────────────────────────────────────────────────────

    private fun setGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        binding.tvGreeting.text = when {
            hour < 12 -> getString(com.example.snapandcook.R.string.home_greeting_morning)
            hour < 18 -> getString(com.example.snapandcook.R.string.home_greeting_afternoon)
            else      -> getString(com.example.snapandcook.R.string.home_greeting_evening)
        }
    }

    // ── Recent recipes RecyclerView ───────────────────────────────────────────

    private fun setupRecentRecycles() {
        recentAdapter = RecentRecipeAdapter { recipe ->
            // Tapping a recent recipe opens the saved-recipes screen
            startActivity(Intent(this, SavedRecipesActivity::class.java))
        }
        binding.rvRecent.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = recentAdapter
        }
    }

    // ── Click listeners ───────────────────────────────────────────────────────

    private fun setupClickListeners() {
        binding.cardCamera.setOnClickListener { requestCameraAndLaunch() }
        binding.cardGallery.setOnClickListener { requestStorageAndLaunch() }
        binding.btnSaved.setOnClickListener {
            startActivity(Intent(this, SavedRecipesActivity::class.java))
        }
    }

    // ── ViewModel observation ─────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.recentRecipes.observe(this) { recipes ->
            if (recipes.isEmpty()) {
                binding.tvNoRecent.show()
                binding.rvRecent.gone()
            } else {
                binding.tvNoRecent.gone()
                binding.rvRecent.show()
                recentAdapter.submitList(recipes)
            }
        }
    }

    // ── Permission + launch helpers ───────────────────────────────────────────

    private fun requestCameraAndLaunch() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED -> launchCamera()
            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun requestStorageAndLaunch() {
        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> {
                // Android 13+: READ_MEDIA_IMAGES is auto-granted via the picker
                galleryLauncher.launch("image/*")
            }
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED -> {
                galleryLauncher.launch("image/*")
            }
            else -> storagePermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    private fun launchCamera() {
        startActivity(Intent(this, CameraActivity::class.java))
    }
}
