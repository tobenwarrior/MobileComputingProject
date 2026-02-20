package com.example.snapandcook.ui.cooking

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.snapandcook.R
import com.example.snapandcook.databinding.ActivityCookingModeBinding
import com.example.snapandcook.util.gone
import com.example.snapandcook.util.show
import com.example.snapandcook.util.toast
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.Locale

/**
 * Step 4 — Cooking Mode.
 *
 * Displays recipe steps one at a time with:
 *  - Large, high-contrast text for kitchen use.
 *  - Android TTS (Text-To-Speech) to read each step aloud automatically.
 *  - Voice commands: say "next", "previous", or "repeat" hands-free.
 *  - Equipment chips showing tools needed for each step.
 *  - Prev / Next navigation buttons for manual control.
 *  - A progress bar showing completion across all steps.
 *  - The screen stays on (FLAG_KEEP_SCREEN_ON) while cooking.
 */
class CookingModeActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var binding: ActivityCookingModeBinding
    private val viewModel: CookingViewModel by viewModels()

    // TTS
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    // Voice recognition
    private var speechRecognizer: SpeechRecognizer? = null
    private var voiceEnabled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        binding = ActivityCookingModeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra(EXTRA_TITLE) ?: ""
        val steps = intent.getStringArrayListExtra(EXTRA_STEPS) ?: arrayListOf()
        val equipmentJson = intent.getStringExtra(EXTRA_EQUIPMENT) ?: "[]"
        val equipment: List<List<String>> = try {
            val type = object : TypeToken<List<List<String>>>() {}.type
            Gson().fromJson(equipmentJson, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }

        binding.tvRecipeTitle.text = title
        viewModel.loadSteps(steps, equipment)

        initTts()
        initSpeechRecognizer()
        setupListeners()
        observeViewModel()
    }

    // ── TTS ───────────────────────────────────────────────────────────────────

    private fun initTts() {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = tts?.setLanguage(Locale.ENGLISH)
                ttsReady = result != TextToSpeech.LANG_MISSING_DATA
                        && result != TextToSpeech.LANG_NOT_SUPPORTED
                if (!ttsReady) {
                    toast(getString(R.string.cooking_tts_unavailable))
                } else {
                    tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) {
                            // Don't stop microphone - allow voice commands during TTS
                        }

                        override fun onDone(utteranceId: String?) {
                            if (voiceEnabled) {
                                binding.root.postDelayed({ startListening() }, 300)
                            }
                        }

                        @Deprecated("Deprecated in Java")
                        override fun onError(utteranceId: String?) {
                            if (voiceEnabled) {
                                binding.root.postDelayed({ startListening() }, 300)
                            }
                        }
                    })
                    if (viewModel.isTtsEnabled.value == true) {
                        speakCurrentStep()
                    }
                }
            } else {
                ttsReady = false
                toast(getString(R.string.cooking_tts_unavailable))
            }
        }
    }

    override fun onInit(status: Int) {
    }

    private fun speakCurrentStep() {
        if (!ttsReady) return
        val text = viewModel.getCurrentStep() ?: return
        // Don't stop voice recognition - allow commands during TTS
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "step_utterance")
    }

    private fun stopSpeaking() {
        tts?.stop()
    }

    // ── Voice recognition ─────────────────────────────────────────────────────

    private fun initSpeechRecognizer() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onResults(results: Bundle) {
                val matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val allText = matches?.joinToString(" ")?.lowercase() ?: ""
                
                // Check all recognition candidates for keywords
                when {
                    allText.contains("next") -> { stopSpeaking(); viewModel.nextStep() }
                    allText.contains("previous") || allText.contains("back") -> { stopSpeaking(); viewModel.prevStep() }
                    allText.contains("repeat") || allText.contains("again") -> speakCurrentStep()
                    else -> { /* no command recognized, just restart listening */ }
                }
                if (voiceEnabled) startListening()
            }

            override fun onError(error: Int) {
                if (voiceEnabled) {
                    if (error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY) {
                        binding.root.postDelayed({ if (voiceEnabled) startListening() }, 500)
                    } else {
                        startListening()
                    }
                }
            }
        })
    }

    private fun startListening() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.ENGLISH)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun toggleVoice() {
        if (!voiceEnabled) {
            if (!SpeechRecognizer.isRecognitionAvailable(this)) {
                toast(getString(R.string.cooking_voice_unavailable))
                return
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.RECORD_AUDIO), RC_AUDIO
                )
                return
            }
            voiceEnabled = true
            updateVoiceButton()
            binding.tvVoiceHint.show()
            startListening()
        } else {
            voiceEnabled = false
            speechRecognizer?.stopListening()
            updateVoiceButton()
            binding.tvVoiceHint.gone()
        }
    }

    private fun updateVoiceButton() {
        binding.btnVoice.setImageResource(
            if (voiceEnabled) R.drawable.ic_mic else R.drawable.ic_mic_off
        )
        binding.btnVoice.contentDescription = getString(
            if (voiceEnabled) R.string.cooking_btn_voice_on else R.string.cooking_btn_voice_off
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_AUDIO &&
            grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED
        ) {
            voiceEnabled = true
            updateVoiceButton()
            startListening()
        }
    }

    // ── Equipment chips ────────────────────────────────────────────────────────

    private fun updateEquipmentChips() {
        val equipment = viewModel.getCurrentEquipment()
        if (equipment.isEmpty()) {
            binding.scrollEquipment.gone()
            return
        }
        binding.scrollEquipment.show()
        binding.llEquipment.removeAllViews()
        val density = resources.displayMetrics.density
        val paddingH = (14 * density).toInt()
        val paddingV = (7 * density).toInt()
        val marginEnd = (8 * density).toInt()
        equipment.forEach { name ->
            val chip = TextView(this).apply {
                text = name
                textSize = 13f
                setTextColor(ContextCompat.getColor(this@CookingModeActivity, R.color.text_secondary))
                background = ContextCompat.getDrawable(this@CookingModeActivity, R.drawable.bg_chip)
                setPadding(paddingH, paddingV, paddingH, paddingV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).also { it.marginEnd = marginEnd }
            }
            binding.llEquipment.addView(chip)
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    override fun onPause() {
        super.onPause()
        stopSpeaking()
        if (voiceEnabled) speechRecognizer?.stopListening()
    }

    override fun onResume() {
        super.onResume()
        if (voiceEnabled) startListening()
    }

    override fun onDestroy() {
        tts?.shutdown()
        speechRecognizer?.destroy()
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

        binding.btnVoice.setOnClickListener { toggleVoice() }
    }

    // ── ViewModel observation ─────────────────────────────────────────────────

    private fun observeViewModel() {
        viewModel.currentStepIndex.observe(this) { idx ->
            val total = viewModel.totalSteps
            val step = viewModel.getCurrentStep() ?: return@observe

            binding.tvStepNumber.text = "${idx + 1}"
            binding.tvStepBody.text = step
            binding.tvStepCounter.text = getString(R.string.cooking_step_progress, idx + 1, total)

            val pct = if (total > 0) ((idx + 1).toFloat() / total * 100).toInt() else 0
            binding.progressSteps.progress = pct

            binding.btnPrev.isEnabled = idx > 0

            updateEquipmentChips()

            if (viewModel.isTtsEnabled.value == true) {
                speakCurrentStep()
            }
        }

        viewModel.isDone.observe(this) { done ->
            if (done) {
                binding.cardStep.gone()
                binding.layoutDone.show()
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
        private const val EXTRA_EQUIPMENT = "extra_equipment"
        private const val RC_AUDIO = 101

        fun start(
            context: Context,
            title: String,
            steps: List<String>,
            equipment: List<List<String>> = emptyList()
        ) {
            context.startActivity(
                Intent(context, CookingModeActivity::class.java).apply {
                    putExtra(EXTRA_TITLE, title)
                    putStringArrayListExtra(EXTRA_STEPS, ArrayList(steps))
                    putExtra(EXTRA_EQUIPMENT, Gson().toJson(equipment))
                }
            )
        }
    }
}
