package com.ainotebuddy.app.voice

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import java.util.Locale
import com.ainotebuddy.app.voice.model.VoiceRecognitionResult
import com.ainotebuddy.app.voice.model.VoiceCommand

/**
 * Service for handling voice commands and speech recognition
 */
class VoiceCommandService(private val context: Context) {
    
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    companion object {
        private const val TAG = "VoiceCommandService"
        
        // Supported voice commands
        const val COMMAND_CREATE_NOTE = "create note"
        const val COMMAND_ADD_TO_NOTE = "add to note"
        const val COMMAND_SEARCH = "search for"
        const val COMMAND_OPEN_NOTE = "open note"
        const val COMMAND_DELETE_NOTE = "delete note"
        const val COMMAND_ARCHIVE = "archive"
        const val COMMAND_SET_REMINDER = "remind me"
        const val COMMAND_ADD_TAG = "add tag"
        
        // Error codes
        const val ERROR_AUDIO = "error_audio"
        const val ERROR_CLIENT = "error_client"
        const val ERROR_INSUFFICIENT_PERMISSIONS = "error_permissions"
        const val ERROR_NETWORK = "error_network"
        const val ERROR_NETWORK_TIMEOUT = "error_network_timeout"
        const val ERROR_NO_MATCH = "error_no_match"
        const val ERROR_RECOGNIZER_BUSY = "error_recognizer_busy"
        const val ERROR_SERVER = "error_server"
        const val ERROR_SPEECH_TIMEOUT = "error_speech_timeout"
    }
    
    /**
     * Start listening for voice commands
     */
    fun startListening(language: String = Locale.getDefault().language): Flow<VoiceRecognitionResult> = callbackFlow {
        val channel = this // capture ProducerScope to use inside callbacks
        if (isListening) {
            close()
            return@callbackFlow
        }
        
        // Initialize speech recognizer if needed
        if (speechRecognizer == null) {
            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                channel.trySend(VoiceRecognitionResult.Error("Speech recognition not available")).isSuccess
                close()
                return@callbackFlow
            }
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener(channel))
            }
        }
        
        // Create recognition intent
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
        }
        
        // Start listening
        isListening = true
        speechRecognizer?.startListening(intent)
        
        // Send listening started event
        channel.trySend(VoiceRecognitionResult.ListeningStarted).isSuccess
        
        // Clean up when the flow is cancelled
        awaitClose {
            try {
                speechRecognizer?.stopListening()
                speechRecognizer?.destroy()
                speechRecognizer = null
                isListening = false
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up speech recognizer", e)
            }
        }
    }.flowOn(Dispatchers.IO)
    
    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        try {
            speechRecognizer?.stopListening()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition", e)
        }
    }
    
    /**
     * Cancel the current listening session
     */
    fun cancel() {
        try {
            speechRecognizer?.cancel()
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling speech recognition", e)
        }
    }
    
    /**
     * Create a recognition listener for handling speech recognition events
     */
    private fun createRecognitionListener(channel: kotlinx.coroutines.channels.ProducerScope<VoiceRecognitionResult>): RecognitionListener {
        return object : RecognitionListener {
            private var isFinal = false
            
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "Ready for speech")
            }
            
            override fun onBeginningOfSpeech() {
                Log.d(TAG, "Speech started")
            }
            
            override fun onRmsChanged(rmsdB: Float) {
                // Handle volume changes for visual feedback
            }
            
            override fun onBufferReceived(buffer: ByteArray?) {
                // Handle audio buffer
            }
            
            override fun onEndOfSpeech() {
                Log.d(TAG, "Speech ended")
                isListening = false
            }
            
            override fun onError(error: Int) {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> ERROR_AUDIO
                    SpeechRecognizer.ERROR_CLIENT -> ERROR_CLIENT
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> ERROR_INSUFFICIENT_PERMISSIONS
                    SpeechRecognizer.ERROR_NETWORK -> ERROR_NETWORK
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> ERROR_NETWORK_TIMEOUT
                    SpeechRecognizer.ERROR_NO_MATCH -> ERROR_NO_MATCH
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> ERROR_RECOGNIZER_BUSY
                    SpeechRecognizer.ERROR_SERVER -> ERROR_SERVER
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> ERROR_SPEECH_TIMEOUT
                    else -> "Unknown error: $error"
                }
                Log.e(TAG, "Speech recognition error: $errorMsg")
                // Send error through the flow
                // Note: We can't use trySend here as it might be called after the flow is closed
            }
            
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty()) {
                    val bestMatch = matches[0]
                    val confidenceScore = confidence?.getOrNull(0) ?: 0f
                    Log.d(TAG, "Speech recognition result: $bestMatch (confidence: $confidenceScore)")
                    
                    // Process the recognized text as a command
                    val command = processVoiceCommand(bestMatch, confidenceScore)
                    // Emit result; ignore return value
                    channel.trySend(VoiceRecognitionResult.Result(text = bestMatch, confidence = confidenceScore, command = command)).isSuccess
                } else {
                    Log.d(TAG, "No speech recognition results")
                }
                
                isFinal = true
            }
            
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val confidence = partialResults?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
                
                if (!matches.isNullOrEmpty() && !isFinal) {
                    val partialText = matches[0]
                    val confidenceScore = confidence?.getOrNull(0) ?: 0f
                    // Send partial result through the flow
                    // Emit partial; ignore return value
                    channel.trySend(VoiceRecognitionResult.PartialResult(text = partialText, confidence = confidenceScore)).isSuccess
                }
            }
            
            override fun onEvent(eventType: Int, params: Bundle?) {
                // Handle additional events if needed
            }
        }
    }
    
    /**
     * Process the recognized text as a voice command
     */
    private fun processVoiceCommand(text: String, confidence: Float): com.ainotebuddy.app.voice.model.VoiceCommand {
        val lowerText = text.lowercase(Locale.getDefault())
        
        return when {
            lowerText.startsWith(COMMAND_CREATE_NOTE) -> {
                val noteContent = text.substringAfter(COMMAND_CREATE_NOTE).trim()
                VoiceCommand.CreateNote(noteContent, confidence)
            }
            lowerText.startsWith(COMMAND_ADD_TO_NOTE) -> {
                val contentToAdd = text.substringAfter(COMMAND_ADD_TO_NOTE).trim()
                VoiceCommand.AddToNote(contentToAdd, confidence)
            }
            lowerText.startsWith(COMMAND_SEARCH) -> {
                val query = text.substringAfter(COMMAND_SEARCH).trim()
                VoiceCommand.Search(query, confidence)
            }
            lowerText.startsWith(COMMAND_OPEN_NOTE) -> {
                val noteTitle = text.substringAfter(COMMAND_OPEN_NOTE).trim()
                VoiceCommand.OpenNote(noteTitle, confidence)
            }
            lowerText.startsWith(COMMAND_DELETE_NOTE) -> {
                val noteTitle = text.substringAfter(COMMAND_DELETE_NOTE).trim()
                VoiceCommand.DeleteNote(noteTitle, confidence)
            }
            lowerText.startsWith(COMMAND_ARCHIVE) -> {
                val noteTitle = text.substringAfter(COMMAND_ARCHIVE).trim()
                VoiceCommand.ArchiveNote(noteTitle, confidence)
            }
            lowerText.startsWith(COMMAND_SET_REMINDER) -> {
                val reminderText = text.substringAfter(COMMAND_SET_REMINDER).trim()
                VoiceCommand.SetReminder(reminderText, confidence)
            }
            lowerText.startsWith(COMMAND_ADD_TAG) -> {
                val tagText = text.substringAfter(COMMAND_ADD_TAG).trim()
                VoiceCommand.AddTag(tagText, confidence)
            }
            else -> VoiceCommand.Unknown(text, confidence)
        }
    }
    
    /**
     * Check if the device is in a call
     */
    private fun isInCall(): Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audioManager.mode == AudioManager.MODE_IN_CALL || 
               audioManager.mode == AudioManager.MODE_IN_COMMUNICATION
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer", e)
        }
    }
}
