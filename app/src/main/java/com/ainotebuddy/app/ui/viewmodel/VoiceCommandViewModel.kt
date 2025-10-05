package com.ainotebuddy.app.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.speech.RecognizerIntent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.voice.VoiceCommandService
import com.ainotebuddy.app.voice.model.VoiceCommand
import com.ainotebuddy.app.voice.model.VoiceRecognitionResult
import com.ainotebuddy.app.voice.model.VoiceRecognitionSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
// import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for handling voice command UI state and interactions
 */
@HiltViewModel
class VoiceCommandViewModel @Inject constructor(
    application: Application,
    private val voiceCommandService: VoiceCommandService
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow<VoiceCommandUiState>(VoiceCommandUiState.Idle)
    val uiState: StateFlow<VoiceCommandUiState> = _uiState.asStateFlow()

    private var recognitionJob: Job? = null
    var isListening by mutableStateOf(false)
        private set

    private val settings = VoiceRecognitionSettings()
    // Remove Timber usage to avoid missing dependency

    /**
     * Start listening for voice commands
     */
    fun startListening() {
        if (isListening) {
            stopListening()
            return
        }

        if (!isSpeechRecognitionAvailable()) {
            _uiState.value = VoiceCommandUiState.Error("Speech recognition not available")
            return
        }

        isListening = true
        _uiState.value = VoiceCommandUiState.Listening

        recognitionJob = voiceCommandService.startListening()
            .onEach { result ->
                when (result) {
                    is VoiceRecognitionResult.ListeningStarted -> {
                        _uiState.value = VoiceCommandUiState.Listening
                    }
                    is VoiceRecognitionResult.PartialResult -> {
                        _uiState.value = VoiceCommandUiState.Recognizing(
                            text = result.text,
                            confidence = result.confidence
                        )
                    }
                    is VoiceRecognitionResult.Result -> {
                        handleVoiceCommand(result.command)
                    }
                    is VoiceRecognitionResult.Error -> {
                        _uiState.value = VoiceCommandUiState.Error(
                            result.message ?: "An error occurred"
                        )
                        isListening = false
                    }
                    VoiceRecognitionResult.Cancelled -> {
                        _uiState.value = VoiceCommandUiState.Idle
                        isListening = false
                    }
                    VoiceRecognitionResult.NoMatch -> {
                        _uiState.value = VoiceCommandUiState.Error("No speech detected")
                        isListening = false
                    }
                    VoiceRecognitionResult.PermissionRequired -> {
                        _uiState.value = VoiceCommandUiState.PermissionRequired
                        isListening = false
                    }
                    VoiceRecognitionResult.NotAvailable -> {
                        _uiState.value = VoiceCommandUiState.Error("Speech recognition not available")
                        isListening = false
                    }
                }
            }
            .catch { exception ->
                _uiState.value = VoiceCommandUiState.Error(exception.message ?: "Unknown error")
                isListening = false
            }
            .launchIn(viewModelScope)
    }

    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        recognitionJob?.cancel()
        voiceCommandService.stopListening()
        isListening = false
        if (_uiState.value !is VoiceCommandUiState.CommandExecuted) {
            _uiState.value = VoiceCommandUiState.Idle
        }
    }

    /**
     * Cancel the current voice recognition session
     */
    fun cancelListening() {
        recognitionJob?.cancel()
        voiceCommandService.cancel()
        isListening = false
        _uiState.value = VoiceCommandUiState.Idle
    }

    /**
     * Reset the UI state to Idle
     */
    fun resetState() {
        _uiState.value = VoiceCommandUiState.Idle
    }

    /**
     * Handle the recognized voice command
     */
    private fun handleVoiceCommand(command: VoiceCommand) {
        when (command) {
            is VoiceCommand.CreateNote -> {
                _uiState.value = VoiceCommandUiState.CommandExecuted(
                    command = "Create Note",
                    result = command.rawText,
                    success = true
                )
                // TODO: Create note with command.content
            }
            is VoiceCommand.AddToNote -> {
                _uiState.value = VoiceCommandUiState.CommandExecuted(
                    command = "Add to Note",
                    result = command.rawText,
                    success = true
                )
                // TODO: Add content to current note
            }
            is VoiceCommand.Search -> {
                _uiState.value = VoiceCommandUiState.CommandExecuted(
                    command = "Search",
                    result = command.query,
                    success = true
                )
                // TODO: Execute search with command.query
            }
            is VoiceCommand.OpenNote -> {
                _uiState.value = VoiceCommandUiState.CommandExecuted(
                    command = "Open Note",
                    result = command.noteTitle,
                    success = true
                )
                // TODO: Open note with command.noteTitle
            }
            is VoiceCommand.DeleteNote -> {
                _uiState.value = VoiceCommandUiState.ConfirmationRequired(
                    command = "Delete Note",
                    message = "Are you sure you want to delete '${command.noteTitle}'?",
                    positiveAction = { confirmDeleteNote(command.noteTitle) },
                    negativeAction = { cancelListening() }
                )
            }
            is VoiceCommand.ArchiveNote -> {
                _uiState.value = VoiceCommandUiState.ConfirmationRequired(
                    command = "Archive Note",
                    message = "Are you sure you want to archive '${command.noteTitle}'?",
                    positiveAction = { confirmArchiveNote(command.noteTitle) },
                    negativeAction = { cancelListening() }
                )
            }
            is VoiceCommand.SetReminder -> {
                _uiState.value = VoiceCommandUiState.CommandExecuted(
                    command = "Set Reminder",
                    result = command.reminderText,
                    success = true
                )
                // TODO: Set reminder with command.reminderText
            }
            is VoiceCommand.AddTag -> {
                _uiState.value = VoiceCommandUiState.CommandExecuted(
                    command = "Add Tag",
                    result = command.tagText,
                    success = true
                )
                // TODO: Add tag with command.tagText
            }
            is VoiceCommand.GoHome -> {
                _uiState.value = VoiceCommandUiState.Navigation("Home")
                // TODO: Navigate to home
            }
            is VoiceCommand.GoBack -> {
                _uiState.value = VoiceCommandUiState.Navigation("Back")
                // TODO: Navigate back
            }
            is VoiceCommand.OpenSettings -> {
                _uiState.value = VoiceCommandUiState.Navigation("Settings")
                // TODO: Navigate to settings
            }
            is VoiceCommand.Help -> {
                _uiState.value = VoiceCommandUiState.Help
            }
            is VoiceCommand.Unknown -> {
                _uiState.value = VoiceCommandUiState.Error("Command not recognized")
            }
            is VoiceCommand.Cancel -> {
                cancelListening()
            }
        }
        
        // Stop listening after handling the command
        isListening = false
    }

    /**
     * Confirm and execute note deletion
     */
    private fun confirmDeleteNote(noteTitle: String) {
        _uiState.value = VoiceCommandUiState.Processing("Deleting note")
        // TODO: Implement actual note deletion
        _uiState.value = VoiceCommandUiState.CommandExecuted(
            command = "Note Deleted",
            result = "Successfully deleted '$noteTitle'",
            success = true
        )
    }

    /**
     * Confirm and execute note archiving
     */
    private fun confirmArchiveNote(noteTitle: String) {
        _uiState.value = VoiceCommandUiState.Processing("Archiving note")
        // TODO: Implement actual note archiving
        _uiState.value = VoiceCommandUiState.CommandExecuted(
            command = "Note Archived",
            result = "Successfully archived '$noteTitle'",
            success = true
        )
    }

    /**
     * Check if speech recognition is available on the device
     */
    private fun isSpeechRecognitionAvailable(): Boolean {
        val context = getApplication<Application>()
        val packageManager = context.packageManager
        val activities = packageManager.queryIntentActivities(
            Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0
        )
        return activities.isNotEmpty()
    }

    /**
     * Open the speech recognition settings screen
     */
    fun openSpeechRecognitionSettings() {
        val intent = Intent(Settings.ACTION_VOICE_INPUT_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        getApplication<Application>().startActivity(intent)
    }

    override fun onCleared() {
        super.onCleared()
        recognitionJob?.cancel()
        voiceCommandService.destroy()
    }
}

/**
 * Sealed class representing the UI state for voice commands
 */
sealed class VoiceCommandUiState {
    object Idle : VoiceCommandUiState()
    object Listening : VoiceCommandUiState()
    data class Recognizing(val text: String, val confidence: Float) : VoiceCommandUiState()
    data class Processing(val message: String) : VoiceCommandUiState()
    data class CommandExecuted(val command: String, val result: String, val success: Boolean) : VoiceCommandUiState()
    data class Error(val message: String) : VoiceCommandUiState()
    data class ConfirmationRequired(
        val command: String,
        val message: String,
        val positiveAction: () -> Unit,
        val negativeAction: () -> Unit
    ) : VoiceCommandUiState()
    data class Navigation(val destination: String) : VoiceCommandUiState()
    object Help : VoiceCommandUiState()
    object PermissionRequired : VoiceCommandUiState()
}
