package com.ainotebuddy.app.features

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlin.random.Random
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.util.*
import javax.inject.Inject

data class VoiceRecordingState(
    val isRecording: Boolean = false,
    val isListening: Boolean = false,
    val transcribedText: String = "",
    val audioLevel: Float = 0f,
    val duration: Long = 0L,
    val error: String? = null,
    val hasPermission: Boolean = false
)

@HiltViewModel
class VoiceNoteCaptureViewModel @Inject constructor(
    private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(VoiceRecordingState())
    val state: StateFlow<VoiceRecordingState> = _state.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var recordingStartTime: Long = 0L

    init {
        checkPermissions()
        initializeSpeechRecognizer()
    }

    private fun checkPermissions() {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED

        _state.value = _state.value.copy(hasPermission = hasPermission)
    }

    fun startVoiceRecognition() {
        if (!_state.value.hasPermission) {
            _state.value = _state.value.copy(error = "Microphone permission required")
            return
        }

        try {
            val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }

            _state.value = _state.value.copy(
                isListening = true,
                error = null,
                transcribedText = ""
            )

            speechRecognizer?.startListening(intent)
            startDurationTimer()

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isListening = false,
                error = "Failed to start voice recognition: ${e.message}"
            )
        }
    }

    fun stopVoiceRecognition() {
        speechRecognizer?.stopListening()
        _state.value = _state.value.copy(
            isListening = false,
            duration = 0L
        )
    }

    fun startAudioRecording() {
        if (!_state.value.hasPermission) {
            _state.value = _state.value.copy(error = "Microphone permission required")
            return
        }

        try {
            recordingFile = File(context.cacheDir, "voice_note_${System.currentTimeMillis()}.m4a")
            
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(recordingFile?.absolutePath)
                prepare()
                start()
            }

            recordingStartTime = System.currentTimeMillis()
            _state.value = _state.value.copy(
                isRecording = true,
                error = null,
                duration = 0L
            )

            startDurationTimer()
            simulateAudioLevels()

        } catch (e: IOException) {
            _state.value = _state.value.copy(
                isRecording = false,
                error = "Failed to start recording: ${e.message}"
            )
        }
    }

    fun stopAudioRecording(): String? {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null

            _state.value = _state.value.copy(
                isRecording = false,
                audioLevel = 0f,
                duration = 0L
            )

            return recordingFile?.absolutePath

        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isRecording = false,
                error = "Failed to stop recording: ${e.message}"
            )
            return null
        }
    }

    private fun initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        _state.value = _state.value.copy(isListening = true)
                    }

                    override fun onBeginningOfSpeech() {
                        // Speech input detected
                    }

                    override fun onRmsChanged(rmsdB: Float) {
                        // Update audio level for visual feedback
                        _state.value = _state.value.copy(audioLevel = rmsdB / 10f)
                    }

                    override fun onBufferReceived(buffer: ByteArray?) {
                        // Audio buffer received
                    }

                    override fun onEndOfSpeech() {
                        // Speech input finished
                    }

                    override fun onError(error: Int) {
                        val errorMessage = when (error) {
                            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                            SpeechRecognizer.ERROR_NETWORK -> "Network error"
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                            SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
                            SpeechRecognizer.ERROR_SERVER -> "Server error"
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
                            else -> "Unknown error"
                        }
                        
                        _state.value = _state.value.copy(
                            isListening = false,
                            error = errorMessage,
                            duration = 0L
                        )
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val recognizedText = matches?.firstOrNull() ?: ""
                        
                        _state.value = _state.value.copy(
                            isListening = false,
                            transcribedText = recognizedText,
                            duration = 0L
                        )
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partialText = matches?.firstOrNull() ?: ""
                        
                        _state.value = _state.value.copy(
                            transcribedText = partialText
                        )
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {
                        // Additional events
                    }
                })
            }
        }
    }

    private fun startDurationTimer() {
        viewModelScope.launch {
            while (_state.value.isRecording || _state.value.isListening) {
                delay(100)
                val elapsed = if (_state.value.isRecording) {
                    System.currentTimeMillis() - recordingStartTime
                } else {
                    _state.value.duration + 100
                }
                _state.value = _state.value.copy(duration = elapsed)
            }
        }
    }

    private fun simulateAudioLevels() {
        viewModelScope.launch {
            while (_state.value.isRecording) {
                delay(50)
                // Simulate audio level changes for visual feedback
                val level = 0.1f + Random.nextFloat() * 0.9f
                _state.value = _state.value.copy(audioLevel = level)
            }
        }
    }
    // Removed stray UI block left inside the ViewModel class
}

private fun formatDuration(millis: Long): String {
    val seconds = (millis / 1000) % 60
    val minutes = (millis / (1000 * 60)) % 60
    return String.format("%02d:%02d", minutes, seconds)
}