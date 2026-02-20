package com.example.snapandcook.ui.cooking

import android.content.Context
import android.content.Intent
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.WindowManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.snapandcook.R
import com.example.snapandcook.databinding.ActivityCookingModeBinding
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show
import com.example.snapandcook.util.toast
import java.util.Locale

/**
 * Step 4 — Cooking Mode.
 *
 * Displays recipe steps one at a time with:
 *  - Large, high-contrast text for kitchen use.
 *  - Android TTS (Text-To-Speech) to read each step aloud automatically.
 *  - Proximity sensor: waving a hand over the phone advances to the next step (hands-free).
 *  - Prev / Next navigation buttons for manual control.
 *  - A progress bar showing completion across all steps.
 *  - The screen stays on (FLAG_KEEP_SCREEN_ON) while cooking.
 */
class CookingModeActivity : AppCompatActivity(), TextToSpeech.OnInitListener, SensorEventListener {

    private lateinit var binding: ActivityCookingModeBinding
    private val viewModel: CookingViewModel by viewModels()

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Proximity sensor
    private lateinit var sensorManager: SensorManager
    private var proximitySensor: Sensor? = null
    private var proximityTriggered = false  // debounce flag

    // Keep screen on while cooking
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityCookingModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val steps = intent.getStringArrayListExtra(EXTRA_STEPS) ?: arrayListOf()

        binding.tvRecipeTitle.text = title

        viewModel.loadSteps(steps)

        initTts()
        initProximitySensor()
        setupListeners()
        observeViewModel()
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    private fun initTts() {
        tts = TextToSpeech(this, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.ENGLISH)
            ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                    && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!ttsReady) {
                toast(getString(R.string.cooking_tts_unavailable))
            } else {
                // Read the first step aloud immediately if TTS is on
                if (viewModel.isTtsEnabled.value == true) {
                    speakCurrentStep()
                }
            }
        } else {
            ttsReady = false
            toast(getString(R.string.cooking_tts_unavailable))
        }
    }

    private fun speakCurrentStep() {
        if (!ttsReady) return
        val text = viewModel.getCurrentStep() ?: return
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step_utterance")
    }

    private fun stopSpeaking() {
        tts?.stop()
    }

    // ── Proximity Sensor ──────────────────────────────────────────────────────

    private fun initProximitySensor() {
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event ?: return
        if (event.sensor.type == Sensor.TYPE_PROXIMITY) {
            val distance = event.values[0]
            val maxRange = proximitySensor?.maximumRange ?: 5f
            val isNear = distance < maxRange * 0.5f

            if (isNear && !proximityTriggered && viewModel.isDone.value == false) {
                proximityTriggered = true
                // Advance to next step
                viewModel.nextStep()
            } else if (!isNear) {
                // Reset debounce when hand is removed
                proximityTriggered = false
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    // ── Lifecycle sensor registration ─────────────────────────────────────────

    override fun onResume() {
        super.onResume()
        proximitySensor?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
        stopSpeaking()
    }

    override fun onDestroy() {
        tts?.shutdown()
        super.onDestroy()
    }

    // ── Listeners ─────────────────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            stopSpeaking()
            finish()
        }

        binding.btnPrev.setOnClickListener {
            stopSpeaking()
            viewModel.prevStep()
        }

        binding.btnNext.setOnClickListener {
            if (viewModel.isDone.value == true) {
                finish()
            } else {
                stopSpeaking()
                viewModel.nextStep()
            }
        }

        binding.btnTts.setOnClickListener {
            viewModel.toggleTts()
            if (viewModel.isTtsEnabled.value == true) {
                speakCurrentStep()
            } else {
                stopSpeaking()
            }
        }
    }

    // ── ViewModel observation ─────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.currentStepIndex.observe(this) { idx ->
            val total = viewModel.totalSteps
            val step  = viewModel.getCurrentStep() ?: return@observe

            // Update step number bubble and body text
            binding.tvStepNumber.text = "${idx + 1}"
            binding.tvStepBody.text = step
            binding.tvStepCounter.text = getString(R.string.cooking_step_progress, idx + 1, total)

            // Update progress bar
            val pct = if (total > 0) ((idx + 1).toFloat() / total * 100).toInt() else 0
            binding.progressSteps.progress = pct

            // Prev button enabled only after step 1
            binding.btnPrev.isEnabled = idx > 0

            // Auto-read aloud
            if (viewModel.isTtsEnabled.value == true) {
                speakCurrentStep()
            }
        }

        viewModel.isDone.observe(this) { done ->
            if (done) {
                binding.cardStep.gone()
                binding.layoutDone.show()
                // Keep Next button enabled and relabel it "Done" so user can exit
                binding.btnNext.isEnabled = true
                binding.btnNext.text = getString(R.string.cooking_btn_done)
                binding.btnPrev.isEnabled = true
                binding.progressSteps.progress = 100
                stopSpeaking()
                if (ttsReady) {
                    tts?.speak(
                        "Congratulations! Your dish is ready. Enjoy your meal!",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "done_utterance"
                    )
                }
            } else {
                binding.cardStep.show()
                binding.layoutDone.gone()
                binding.btnNext.isEnabled = true
                binding.btnNext.text = getString(R.string.cooking_btn_next)
            }
        }

        viewModel.isTtsEnabled.observe(this) { enabled ->
            binding.btnTts.setImageResource(
                if (enabled) R.drawable.ic_volume else R.drawable.ic_volume_off
            )
            binding.btnTts.contentDescription = getString(
                if (enabled) R.string.cooking_btn_tts_on else R.string.cooking_btn_tts_off
            )
        }
    }

    companion object {
        private const val EXTRA_TITLE = "extra_title"
        private const val EXTRA_STEPS = "extra_steps"

        fun start(context: Context, title: String, steps: List<String>) {
            context.startActivity(
                Intent(context, CookingModeActivity::class.java).apply {
                    putExtra(EXTRA_TITLE, title)
                    putStringArrayListExtra(EXTRA_STEPS, ArrayList(steps))
                }
            )
        }
    }
}
