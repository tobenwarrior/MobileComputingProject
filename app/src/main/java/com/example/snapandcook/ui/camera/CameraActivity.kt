package com.example.snapandcook.ui.camera

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.OrientationEventListener
import android.view.Surface
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import com.example.snapandcook.databinding.ActivityCameraBinding
import com.example.snapandcook.ui.verification.VerificationActivity
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show
import com.example.snapandcook.util.toast
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Camera screen powered by CameraX.
 *
 * Supports:
 *  - Live viewfinder via [PreviewView]
 *  - Single photo capture via [ImageCapture]
 *  - Flash toggle (auto / off)
 *  - Front / back camera flip
 *  - Gallery images passed in from [MainActivity]
 *
 * After capture (or gallery selection), the URIs are forwarded to [VerificationActivity].
 */
class CameraActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCameraBinding
    private lateinit var cameraExecutor: ExecutorService

    private var imageCapture: ImageCapture? = null
    private var cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var flashMode = ImageCapture.FLASH_MODE_OFF
    private var camera: Camera? = null

    // Accumulated image URIs (camera captures + any gallery picks)
    private val capturedUris = mutableListOf<Uri>()

    // If launched with gallery URIs, go straight to verification
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            capturedUris.addAll(uris)
            updatePhotosCount()
            showAnalyseButton()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraExecutor = Executors.newSingleThreadExecutor()

        // If launched with pre-selected gallery URIs, pass straight through
        val preselected = intent.getStringArrayListExtra(EXTRA_GALLERY_URIS)
        if (!preselected.isNullOrEmpty()) {
            capturedUris.addAll(preselected.map { Uri.parse(it) })
            navigateToVerification()
            return
        }

        startCamera()
        setupListeners()
    }

    // ── CameraX setup ─────────────────────────────────────────────────────────

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindCamera(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCamera(cameraProvider: ProcessCameraProvider) {
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(binding.previewView.surfaceProvider)
        }

        imageCapture = ImageCapture.Builder()
            .setFlashMode(flashMode)
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
            .build()

        // Rotate output to match device orientation
        val orientationListener = object : OrientationEventListener(this) {
            override fun onOrientationChanged(orientation: Int) {
                val rotation = when {
                    orientation in 45..134  -> Surface.ROTATION_270
                    orientation in 135..224 -> Surface.ROTATION_180
                    orientation in 225..314 -> Surface.ROTATION_90
                    else                    -> Surface.ROTATION_0
                }
                imageCapture?.targetRotation = rotation
            }
        }
        orientationListener.enable()

        try {
            cameraProvider.unbindAll()
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
        } catch (e: Exception) {
            Log.e(TAG, "Camera bind failed", e)
        }
    }

    // ── Capture ───────────────────────────────────────────────────────────────

    private fun capturePhoto() {
        val ic = imageCapture ?: return
        val file = File(externalCacheDir, "img_${System.currentTimeMillis()}.jpg")
        val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

        ic.takePicture(outputOptions, cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val uri = output.savedUri ?: Uri.fromFile(file)
                    capturedUris.add(uri)
                    runOnUiThread {
                        updatePhotosCount()
                        showAnalyseButton()
                        toast("Photo captured!")
                    }
                }

                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed", exc)
                    runOnUiThread { toast("Capture failed. Please try again.") }
                }
            }
        )
    }

    // ── UI helpers ────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnBack.setOnClickListener { finish() }

        binding.btnCapture.setOnClickListener { capturePhoto() }

        binding.btnFlash.setOnClickListener {
            flashMode = if (flashMode == ImageCapture.FLASH_MODE_OFF)
                ImageCapture.FLASH_MODE_AUTO else ImageCapture.FLASH_MODE_OFF
            imageCapture?.flashMode = flashMode
            val alpha = if (flashMode == ImageCapture.FLASH_MODE_AUTO) 1f else 0.5f
            binding.btnFlash.alpha = alpha
        }

        binding.btnFlip.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                bindCamera(cameraProviderFuture.get())
            }, ContextCompat.getMainExecutor(this))
        }

        binding.btnGallery.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        binding.btnAnalyze.setOnClickListener {
            if (capturedUris.isEmpty()) {
                toast("Please take or select at least one photo first.")
            } else {
                navigateToVerification()
            }
        }
    }

    private fun updatePhotosCount() {
        val count = capturedUris.size
        if (count > 0) {
            binding.tvPhotosCount.text = getString(
                com.example.snapandcook.R.string.camera_photos_selected, count
            )
            binding.tvPhotosCount.show()
        } else {
            binding.tvPhotosCount.gone()
        }
    }

    private fun showAnalyseButton() {
        binding.btnAnalyze.show()
    }

    private fun navigateToVerification() {
        VerificationActivity.start(this, capturedUris.map { it.toString() })
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val TAG = "CameraActivity"
        const val EXTRA_GALLERY_URIS = "extra_gallery_uris"

        /** Launch from MainActivity with pre-selected gallery URIs. */
        fun startWithGalleryUris(context: Context, uris: List<String>) {
            val intent = Intent(context, CameraActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_GALLERY_URIS, ArrayList(uris))
            }
            context.startActivity(intent)
        }
    }
}
