package com.ainotebuddy.app.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.navigation.VoiceNavigationManager
import com.ainotebuddy.app.voice.VoiceCommandService
import com.ainotebuddy.app.voice.model.VoiceCommand
import com.ainotebuddy.app.voice.model.VoiceNavigationFeedback
import com.ainotebuddy.app.voice.model.VoiceNavigationTarget
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
// import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for the VoiceNoteEditorScreen
 */
@HiltViewModel
class VoiceNoteEditorViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val voiceCommandService: VoiceCommandService,
    private val navigationManager: VoiceNavigationManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceNoteEditorState())
    val uiState: StateFlow<VoiceNoteEditorState> = _uiState.asStateFlow()

    private val _commandResult = Channel<VoiceCommand>(Channel.BUFFERED)
    val commandResult = _commandResult.receiveAsFlow()

    private var recognitionJob: Job? = null
    private var currentNoteId: Long? = null

    init {
        startListening()
    }

    /**
     * Start listening for voice commands
     */
    fun startListening() {
        if (_uiState.value.isListening) return

        recognitionJob?.cancel()
        recognitionJob = viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isListening = true,
                    feedback = VoiceNavigationFeedback.Listening
                )

                voiceCommandService.startListening().collect { result ->
                    when (result) {
                        is com.ainotebuddy.app.voice.model.VoiceRecognitionResult.Result -> {
                            _commandResult.send(result.command)
                        }
                        is com.ainotebuddy.app.voice.model.VoiceRecognitionResult.Error -> {
                            showFeedback(VoiceNavigationFeedback.Error(result.message))
                        }
                        is com.ainotebuddy.app.voice.model.VoiceRecognitionResult.PartialResult -> {
                            // Optionally update UI with partial transcript
                        }
                        else -> { /* Handle other states if needed */ }
                    }
                }
            } catch (e: Exception) {
                showFeedback(VoiceNavigationFeedback.Error("Voice recognition failed: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isListening = false)
            }
        }
    }

    /**
     * Stop listening for voice commands
     */
    fun stopListening() {
        recognitionJob?.cancel()
        _uiState.value = _uiState.value.copy(isListening = false)
    }

    /**
     * Process a recognized voice command
     */
    fun processVoiceCommand(command: VoiceCommand) {
        viewModelScope.launch {
            _commandResult.send(command)
        }
    }

    /**
     * Update the note content
     */
    fun updateContent(newContent: String) {
        _uiState.value = _uiState.value.copy(content = newContent)
    }

    /**
     * Save the current note
     */
    fun saveNote() {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(
                    isProcessing = true,
                    feedback = VoiceNavigationFeedback.Processing
                )

                val content = _uiState.value.content
                if (content.isBlank()) {
                    showFeedback(VoiceNavigationFeedback.Error("Cannot save an empty note"))
                    return@launch
                }

                val noteId: Long = currentNoteId ?: -1L
                val title = content.take(50) + if (content.length > 50) "..." else ""

                if (noteId != -1L) {
                    val existing = noteRepository.getNoteById(noteId)
                    if (existing != null) {
                        noteRepository.updateNote(
                            existing.copy(
                                title = title,
                                content = content,
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        currentNoteId = existing.id
                    } else {
                        val newId = noteRepository.insertNote(
                            com.ainotebuddy.app.data.NoteEntity(
                                title = title,
                                content = content,
                                createdAt = System.currentTimeMillis(),
                                updatedAt = System.currentTimeMillis()
                            )
                        )
                        currentNoteId = newId
                    }
                } else {
                    val newId = noteRepository.insertNote(
                        com.ainotebuddy.app.data.NoteEntity(
                            title = title,
                            content = content,
                            createdAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                    )
                    currentNoteId = newId
                }

                // Update the navigation target to go to the saved note
                _uiState.value = _uiState.value.copy(
                    navigationTarget = VoiceNavigationTarget.NoteDetail(currentNoteId?.toString() ?: ""),
                    feedback = VoiceNavigationFeedback.Success("Note saved successfully")
                )
            } catch (e: Exception) {
                showFeedback(VoiceNavigationFeedback.Error("Failed to save note: ${e.message}"))
            } finally {
                _uiState.value = _uiState.value.copy(isProcessing = false)
            }
        }
    }

    /**
     * Show feedback to the user
     */
    fun showFeedback(feedback: VoiceNavigationFeedback) {
        _uiState.value = _uiState.value.copy(feedback = feedback)
    }

    /**
     * Clear the current navigation target
     */
    fun clearNavigationTarget() {
        _uiState.value = _uiState.value.copy(navigationTarget = null)
    }

    /**
     * Load an existing note
     */
    fun loadNote(noteId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    currentNoteId = note.id
                    _uiState.value = _uiState.value.copy(
                        content = note.content,
                        isLoading = false
                    )
                } else {
                    showFeedback(VoiceNavigationFeedback.Error("Note not found"))
                    _uiState.value = _uiState.value.copy(isLoading = false)
                }
            } catch (e: Exception) {
                showFeedback(VoiceNavigationFeedback.Error("Failed to load note: ${e.message}"))
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        recognitionJob?.cancel()
        voiceCommandService.destroy()
    }
}

/**
 * UI state for the VoiceNoteEditorScreen
 */
data class VoiceNoteEditorState(
    val content: String = "",
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val isLoading: Boolean = false,
    val feedback: VoiceNavigationFeedback = VoiceNavigationFeedback.Idle,
    val navigationTarget: VoiceNavigationTarget? = null,
    val error: String? = null
)
