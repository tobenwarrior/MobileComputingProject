package com.example.snapandcook.ui.cooking

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

/**
 * ViewModel for the Cooking Mode screen.
 *
 * Manages the current step index and TTS state.
 * The actual TTS engine and SensorManager are owned by the Activity (lifecycle-aware).
 */
class CookingViewModel : ViewModel() {

    private val _steps = MutableLiveData<List<String>>(emptyList())
    val steps: LiveData<List<String>> = _steps

    private val _currentStepIndex = MutableLiveData(0)
    val currentStepIndex: LiveData<Int> = _currentStepIndex

    private val _isTtsEnabled = MutableLiveData(true)
    val isTtsEnabled: LiveData<Boolean> = _isTtsEnabled

    private val _isDone = MutableLiveData(false)
    val isDone: LiveData<Boolean> = _isDone

    fun loadSteps(steps: List<String>) {
        _steps.value = steps
        _currentStepIndex.value = 0
        _isDone.value = false
    }

    fun getCurrentStep(): String? {
        val list = _steps.value ?: return null
        val idx = _currentStepIndex.value ?: return null
        return list.getOrNull(idx)
    }

    fun nextStep() {
        val list = _steps.value ?: return
        val idx = _currentStepIndex.value ?: return
        if (idx < list.size - 1) {
            _currentStepIndex.value = idx + 1
        } else {
            _isDone.value = true
        }
    }

    fun prevStep() {
        val idx = _currentStepIndex.value ?: return
        if (idx > 0) {
            _currentStepIndex.value = idx - 1
            _isDone.value = false
        }
    }

    fun toggleTts() {
        _isTtsEnabled.value = !(_isTtsEnabled.value ?: true)
    }

    val totalSteps get() = _steps.value?.size ?: 0
}
