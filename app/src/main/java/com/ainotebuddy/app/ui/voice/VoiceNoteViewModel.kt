package com.ainotebuddy.app.ui.voice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.voice.*
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.data.NoteEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Simplified ViewModel for Voice Note Screen
 */
@HiltViewModel
class VoiceNoteViewModel @Inject constructor(
    private val voiceEngine: VoiceEngine,
    private val noteRepository: NoteRepository
) : ViewModel() {

    // UI-facing voice state used by VoiceNoteScreen
    private val _voiceState = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val voiceState: StateFlow<VoiceState> = _voiceState.asStateFlow()

    // Real-time transcription shown on the screen
    val realTimeTranscription: StateFlow<String> = voiceEngine.transcription.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ""
    )

    // Quick AI insights chips shown under transcription
    private val _voiceInsights = MutableStateFlow<List<VoiceQuickInsight>>(emptyList())
    val voiceInsights: StateFlow<List<VoiceQuickInsight>> = _voiceInsights.asStateFlow()

    // Recording duration in milliseconds
    private val _recordingDuration = MutableStateFlow(0L)
    val recordingDuration: StateFlow<Long> = _recordingDuration.asStateFlow()

    // Processing and error state (internal)
    private val _isProcessing = MutableStateFlow(false)
    val isProcessing = _isProcessing.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    private val _sessionResult = MutableStateFlow<VoiceSessionResult?>(null)
    val sessionResult = _sessionResult.asStateFlow()

    private var timerJob: Job? = null

    fun initialize() {
        // Reserved for future initialization work (permissions, warmups, etc.)
    }

    fun showVoiceSettings() {
        // No-op placeholder; UI will navigate to a settings screen if implemented
    }

    fun startVoiceNote() {
        if (_voiceState.value is VoiceState.Recording) return
        viewModelScope.launch {
            try {
                _error.value = null
                _voiceState.value = VoiceState.Processing // brief transition while engine starts
                val result = voiceEngine.startListening()
                _sessionResult.value = result
                _voiceState.value = VoiceState.Recording
                startTimer()
            } catch (e: Exception) {
                _voiceState.value = VoiceState.Error(e.message ?: "Failed to start recording")
            }
        }
    }

    fun stopVoiceNote() {
        if (_voiceState.value !is VoiceState.Recording && _voiceState.value !is VoiceState.Listening) return
        viewModelScope.launch {
            try {
                _voiceState.value = VoiceState.Processing
                val result = voiceEngine.stopListening()
                _sessionResult.value = result
                stopTimer(reset = false)
                // Optionally push a quick insight based on final transcript
                result.transcription.takeIf { it.isNotBlank() }?.let { finalText ->
                    _voiceInsights.value = _voiceInsights.value + VoiceQuickInsight(
                        type = VoiceQuickInsightType.IMPORTANT_POINT,
                        content = "Captured ${finalText.length.coerceAtMost(40)} chars",
                        confidence = result.confidence,
                        timestamp = System.currentTimeMillis()
                    )
                }
                _voiceState.value = VoiceState.Idle
            } catch (e: Exception) {
                _voiceState.value = VoiceState.Error(e.message ?: "Failed to stop recording")
                stopTimer(reset = false)
            }
        }
    }

    fun pauseVoiceNote() {
        // Simplified pause: mark as Listening (not actively recording) and pause timer
        if (_voiceState.value is VoiceState.Recording) {
            _voiceState.value = VoiceState.Listening
            pauseTimer()
        }
    }

    fun resumeVoiceNote() {
        if (_voiceState.value is VoiceState.Listening) {
            _voiceState.value = VoiceState.Recording
            resumeTimer()
        }
    }

    fun cancelVoiceNote() {
        _voiceState.value = VoiceState.Idle
        stopTimer(reset = true)
    }

    fun processVoiceCommand(command: String) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null
                voiceEngine.processVoiceCommand(command)
            } catch (e: Exception) {
                _error.value = "Failed to process command: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun saveVoiceNote(title: String, content: String) {
        viewModelScope.launch {
            try {
                _isProcessing.value = true
                _error.value = null
                val note = NoteEntity(
                    title = title.ifEmpty { "Voice Note ${System.currentTimeMillis()}" },
                    content = content,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )
                noteRepository.insertNote(note)
            } catch (e: Exception) {
                _error.value = "Failed to save note: ${e.message}"
            } finally {
                _isProcessing.value = false
            }
        }
    }

    fun clearError() { _error.value = null }

    private fun startTimer() {
        stopTimer(reset = true)
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            val start = System.currentTimeMillis()
            while (isActive && (_voiceState.value is VoiceState.Recording)) {
                _recordingDuration.value = System.currentTimeMillis() - start
                delay(250)
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
    }

    private fun resumeTimer() {
        // Resume from current duration
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            val resumeStart = System.currentTimeMillis() - _recordingDuration.value
            while (isActive && (_voiceState.value is VoiceState.Recording)) {
                _recordingDuration.value = System.currentTimeMillis() - resumeStart
                delay(250)
            }
        }
    }

    private fun stopTimer(reset: Boolean) {
        timerJob?.cancel()
        timerJob = null
        if (reset) _recordingDuration.value = 0L
    }
}