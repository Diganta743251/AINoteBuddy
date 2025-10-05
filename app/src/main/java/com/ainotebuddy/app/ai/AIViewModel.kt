package com.ainotebuddy.app.ai

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIViewModel @Inject constructor(
    private val noteRepository: NoteRepository,
    private val preferencesManager: PreferencesManager, 
    private val aiService: AIServiceProvider
) : ViewModel() {

    // UI State
    private val _uiState = MutableStateFlow(AIUIState())
    val uiState: StateFlow<AIUIState> = _uiState.asStateFlow()

    // Suggestions for the current note
    private val _suggestions = mutableStateListOf<AISuggestion>()
    val suggestions: List<AISuggestion> = _suggestions

    // Sentiment analysis cache
    private val _sentimentCache = mutableStateMapOf<String, SentimentAnalysisResult>()
    val sentimentCache: Map<String, SentimentAnalysisResult> = _sentimentCache

    // Voice commands history
    private val _voiceCommandHistory = mutableStateListOf<VoiceCommandResult>()
    val voiceCommandHistory: List<VoiceCommandResult> = _voiceCommandHistory

    // Available voice commands
    val availableVoiceCommands = listOf(
        VoiceCommand(
            id = "create_note",
            command = "Create a new note about [topic]",
            description = "Creates a new note with the given topic",
            example = "Create a new note about team meeting"
        ),
        VoiceCommand(
            id = "search_notes",
            command = "Find notes about [query]",
            description = "Searches for notes containing the query",
            example = "Find notes about project deadline"
        ),
        VoiceCommand(
            id = "set_reminder",
            command = "Remind me to [task] at [time]",
            description = "Sets a reminder for the specified task and time",
            example = "Remind me to call John at 3pm"
        )
    )

    /**
     * Load suggestions for a note
     */
    fun loadSuggestions(note: NoteEntity, context: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isLoading = true,
                error = null
            )
            
            try {
                val newSuggestions = aiService.generateSuggestions(note, context)
                _suggestions.clear()
                _suggestions.addAll(newSuggestions)
                _uiState.value = _uiState.value.copy(
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load suggestions"
                )
            }
        }
    }

    /**
     * Analyze sentiment for a note
     */
    fun analyzeSentiment(note: NoteEntity) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isAnalyzingSentiment = true,
                error = null
            )
            
            try {
                val result = aiService.analyzeSentiment(note)
                _sentimentCache[note.id.toString()] = result
                _uiState.value = _uiState.value.copy(
                    isAnalyzingSentiment = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAnalyzingSentiment = false,
                    error = e.message ?: "Failed to analyze sentiment"
                )
            }
        }
    }

    /**
     * Generate tags for a note
     */
    fun generateTags(note: NoteEntity, existingTags: List<String> = emptyList()): StateFlow<List<String>> {
        val result = MutableStateFlow<List<String>>(emptyList())
        
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isGeneratingTags = true,
                error = null
            )
            
            try {
                val tags = aiService.generateTags(note, existingTags)
                result.value = tags
                _uiState.value = _uiState.value.copy(
                    isGeneratingTags = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isGeneratingTags = false,
                    error = e.message ?: "Failed to generate tags"
                )
                result.value = emptyList()
            }
        }
        
        return result
    }

    /**
     * Process a voice command
     */
    fun processVoiceCommand(command: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isProcessingVoiceCommand = true,
                error = null
            )
            
            try {
                val result = aiService.processVoiceCommand(command)
                _voiceCommandHistory.add(0, result) // Add to beginning of list
                _uiState.value = _uiState.value.copy(
                    isProcessingVoiceCommand = false,
                    lastVoiceCommandResult = result
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isProcessingVoiceCommand = false,
                    error = e.message ?: "Failed to process voice command"
                )
            }
        }
    }

    /**
     * Clear the current error
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Clear all suggestions
     */
    fun clearSuggestions() {
        _suggestions.clear()
    }
}

/**
 * UI State for AI features
 */
data class AIUIState(
    val isLoading: Boolean = false,
    val isAnalyzingSentiment: Boolean = false,
    val isGeneratingTags: Boolean = false,
    val isProcessingVoiceCommand: Boolean = false,
    val error: String? = null,
    val lastVoiceCommandResult: VoiceCommandResult? = null
)
