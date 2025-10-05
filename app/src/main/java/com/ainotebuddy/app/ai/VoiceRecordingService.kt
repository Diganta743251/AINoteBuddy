package com.ainotebuddy.app.ai

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class VoiceRecordingService(private val context: Context) {
    
    private var mediaRecorder: MediaRecorder? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var audioFile: File? = null
    private var isRecording = false
    private var recordingStartTime = 0L
    
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()
    
    private val _recordingTime = MutableStateFlow(0L)
    val recordingTime: StateFlow<Long> = _recordingTime.asStateFlow()
    
    private val _transcribedText = MutableStateFlow("")
    val transcribedText: StateFlow<String> = _transcribedText.asStateFlow()
    
    sealed class RecordingState {
        object Idle : RecordingState()
        object Recording : RecordingState()
        object Paused : RecordingState()
        object Processing : RecordingState()
        data class Error(val message: String) : RecordingState()
    }
    
    fun hasRecordingPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    fun startRecording() {
        if (!hasRecordingPermission()) {
            _recordingState.value = RecordingState.Error("Recording permission not granted")
            return
        }
        
        try {
            createAudioFile()
            setupMediaRecorder()
            mediaRecorder?.start()
            
            isRecording = true
            recordingStartTime = System.currentTimeMillis()
            _recordingState.value = RecordingState.Recording
            
            // Start speech recognition for real-time transcription
            startSpeechRecognition()
            
        } catch (e: IOException) {
            _recordingState.value = RecordingState.Error("Failed to start recording: ${e.message}")
        }
    }
    
    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            isRecording = false
            _recordingState.value = RecordingState.Processing
            
            // Stop speech recognition
            speechRecognizer?.destroy()
            speechRecognizer = null
            
            // Process the recorded audio for final transcription
            processRecordedAudio()
            
        } catch (e: Exception) {
            _recordingState.value = RecordingState.Error("Failed to stop recording: ${e.message}")
        }
    }
    
    fun pauseRecording() {
        if (isRecording) {
            try {
                mediaRecorder?.pause()
                _recordingState.value = RecordingState.Paused
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error("Failed to pause recording: ${e.message}")
            }
        }
    }
    
    fun resumeRecording() {
        if (_recordingState.value == RecordingState.Paused) {
            try {
                mediaRecorder?.resume()
                _recordingState.value = RecordingState.Recording
            } catch (e: Exception) {
                _recordingState.value = RecordingState.Error("Failed to resume recording: ${e.message}")
            }
        }
    }
    
    private fun createAudioFile() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "VOICE_NOTE_$timeStamp.m4a"
        audioFile = File(context.cacheDir, fileName)
    }
    
    private fun setupMediaRecorder() {
        mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            @Suppress("DEPRECATION")
            MediaRecorder()
        }
        
        mediaRecorder?.apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setAudioEncodingBitRate(128000)
            setAudioSamplingRate(44100)
            setOutputFile(audioFile?.absolutePath)
            
            try {
                prepare()
            } catch (e: IOException) {
                throw e
            }
        }
    }
    
    private fun startSpeechRecognition() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            }
            
            speechRecognizer?.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    // Ready to start recognition
                }
                
                override fun onBeginningOfSpeech() {
                    // Speech started
                }
                
                override fun onRmsChanged(rmsdB: Float) {
                    // Audio level changed - could be used for visualization
                }
                
                override fun onBufferReceived(buffer: ByteArray?) {
                    // Audio buffer received
                }
                
                override fun onEndOfSpeech() {
                    // Speech ended
                }
                
                override fun onError(error: Int) {
                    // Recognition error - continue recording even if recognition fails
                }
                
                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { text ->
                        _transcribedText.value = text
                    }
                }
                
                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    matches?.firstOrNull()?.let { text ->
                        _transcribedText.value = text
                    }
                }
                
                override fun onEvent(eventType: Int, params: Bundle?) {
                    // Event occurred
                }
            })
            
            speechRecognizer?.startListening(intent)
        }
    }
    
    private fun processRecordedAudio() {
        // For now, we'll use the transcribed text from speech recognition
        // In a real implementation, you might want to send the audio file to a cloud service
        // for more accurate transcription
        
        if (_transcribedText.value.isEmpty()) {
            // If no real-time transcription, try to transcribe the recorded file
            transcribeAudioFile()
        }
        
        _recordingState.value = RecordingState.Idle
    }
    
    private fun transcribeAudioFile() {
        // This would typically involve sending the audio file to a cloud service
        // For now, we'll just set a placeholder
        _transcribedText.value = "Voice recording completed. Transcription would be processed here."
    }
    
    fun getRecordingTime(): Long {
        return if (isRecording) {
            System.currentTimeMillis() - recordingStartTime
        } else {
            0L
        }
    }
    
    fun clearRecording() {
        _transcribedText.value = ""
        _recordingTime.value = 0L
        _recordingState.value = RecordingState.Idle
        
        audioFile?.delete()
        audioFile = null
    }
    
    fun getTranscribedText(): String {
        return _transcribedText.value
    }
    
    fun cleanup() {
        mediaRecorder?.apply {
            if (isRecording) {
                try {
                    stop()
                } catch (e: Exception) {
                    // Ignore errors during cleanup
                }
            }
            release()
        }
        mediaRecorder = null
        
        speechRecognizer?.destroy()
        speechRecognizer = null
        
        audioFile?.delete()
        audioFile = null
    }
} 