package com.ainotebuddy.app.ui.viewmodel.organization

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.ai.AISmartOrganizationService
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.model.organization.SmartFolder
import com.ainotebuddy.app.data.model.organization.SmartFolder.SmartFolderRule
// Avoid using a custom Result wrapper; we'll use Kotlin Result or simple try/catch in callers
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for AI-powered smart organization features
 */
@HiltViewModel
class AISmartOrganizationViewModel @Inject constructor(
    private val aiSmartOrganizationService: AISmartOrganizationService
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<AISmartOrganizationUiState>(AISmartOrganizationUiState.Idle)
    val uiState: StateFlow<AISmartOrganizationUiState> = _uiState.asStateFlow()
    
    // Current analysis job to allow cancellation
    private var currentAnalysisJob: Job? = null
    
    // State for related notes
    private val _relatedNotes = MutableStateFlow<Map<String, List<Pair<Note, Double>>>>(emptyMap())
    val relatedNotes = _relatedNotes.asStateFlow()
    
    // State for folder suggestions
    private val _suggestedFolders = MutableStateFlow<Map<String, List<Pair<SmartFolder, Double>>>>(emptyMap())
    val suggestedFolders = _suggestedFolders.asStateFlow()
    
    // State for generated templates
    private val _generatedTemplates = MutableStateFlow<Map<String, NoteTemplate>>(emptyMap())
    val generatedTemplates = _generatedTemplates.asStateFlow()
    
    // State for suggested folder rules
    private val _suggestedFolderRules = MutableStateFlow<Map<String, List<SmartFolderRule>>>(emptyMap())
    val suggestedFolderRules = _suggestedFolderRules.asStateFlow()
    
    // State for smart folder suggestions
    private val _smartFolderSuggestions = MutableStateFlow<List<Pair<String, List<SmartFolderRule>>>>(emptyList())
    val smartFolderSuggestions = _smartFolderSuggestions.asStateFlow()
    
    // State for AI processing progress
    var isProcessing by mutableStateOf(false)
        private set
    
    // State for AI processing progress message
    var processingMessage by mutableStateOf("")
        private set
    
    /**
     * Analyze a note and suggest smart folders it should belong to
     */
    fun analyzeNoteForFolders(noteId: String) {
        currentAnalysisJob?.cancel()
        currentAnalysisJob = viewModelScope.launch {
            _uiState.value = AISmartOrganizationUiState.Loading("Analyzing note for smart folders...")
            isProcessing = true
            processingMessage = "Analyzing note content..."
            
            val result = aiSmartOrganizationService.suggestFoldersForNote(noteId)
            result.onSuccess { list ->
                _suggestedFolders.update { current -> current + (noteId to list) }
                _uiState.value = AISmartOrganizationUiState.Success
            }.onFailure { e ->
                _uiState.value = AISmartOrganizationUiState.Error(e.message ?: "Failed to analyze note")
            }
            
            isProcessing = false
        }
    }
    
    /**
     * Find notes related to the given note
     */
    fun findRelatedNotes(noteId: String, limit: Int = 5) {
        currentAnalysisJob?.cancel()
        currentAnalysisJob = viewModelScope.launch {
            _uiState.value = AISmartOrganizationUiState.Loading("Finding related notes...")
            isProcessing = true
            processingMessage = "Searching for related notes..."
            
            val result = aiSmartOrganizationService.findRelatedNotes(noteId, limit)
            result.onSuccess { list ->
                _relatedNotes.update { current -> current + (noteId to list) }
                _uiState.value = AISmartOrganizationUiState.Success
            }.onFailure { e ->
                _uiState.value = AISmartOrganizationUiState.Error(e.message ?: "Failed to find related notes")
            }
            
            isProcessing = false
        }
    }
    
    /**
     * Generate a template from an existing note
     */
    fun generateTemplateFromNote(noteId: String) {
        currentAnalysisJob?.cancel()
        currentAnalysisJob = viewModelScope.launch {
            _uiState.value = AISmartOrganizationUiState.Loading("Generating template from note...")
            isProcessing = true
            processingMessage = "Analyzing note structure..."
            
            val result = aiSmartOrganizationService.generateTemplateFromNote(noteId)
            result.onSuccess { template ->
                _generatedTemplates.update { current -> current + (noteId to template) }
                _uiState.value = AISmartOrganizationUiState.Success
            }.onFailure { e ->
                _uiState.value = AISmartOrganizationUiState.Error(e.message ?: "Failed to generate template")
            }
            
            isProcessing = false
        }
    }
    
    /**
     * Suggest folder rules based on note content
     */
    fun suggestFolderRules(noteId: String) {
        currentAnalysisJob?.cancel()
        currentAnalysisJob = viewModelScope.launch {
            _uiState.value = AISmartOrganizationUiState.Loading("Analyzing note for folder rules...")
            isProcessing = true
            processingMessage = "Analyzing note content..."
            
            val result = aiSmartOrganizationService.suggestFolderRules(noteId)
            result.onSuccess { rules ->
                _suggestedFolderRules.update { current -> current + (noteId to rules) }
                _uiState.value = AISmartOrganizationUiState.Success
            }.onFailure { e ->
                _uiState.value = AISmartOrganizationUiState.Error(e.message ?: "Failed to suggest folder rules")
            }
            
            isProcessing = false
        }
    }
    
    /**
     * Analyze notes and suggest smart folders to create
     */
    fun suggestSmartFolders() {
        currentAnalysisJob?.cancel()
        currentAnalysisJob = viewModelScope.launch {
            _uiState.value = AISmartOrganizationUiState.Loading("Analyzing your notes to suggest smart folders...")
            isProcessing = true
            processingMessage = "Analyzing note collection..."
            
            val result = aiSmartOrganizationService.suggestSmartFolders()
            result.onSuccess { suggestions ->
                _smartFolderSuggestions.value = suggestions
                _uiState.value = AISmartOrganizationUiState.Success
            }.onFailure { e ->
                _uiState.value = AISmartOrganizationUiState.Error(e.message ?: "Failed to suggest smart folders")
            }
            
            isProcessing = false
        }
    }
    
    /**
     * Clear the current UI state
     */
    fun clearState() {
        _uiState.value = AISmartOrganizationUiState.Idle
    }
    
    /**
     * Clear suggestions for a specific note
     */
    fun clearSuggestions(noteId: String) {
        _suggestedFolders.update { it - noteId }
        _relatedNotes.update { it - noteId }
        _generatedTemplates.update { it - noteId }
        _suggestedFolderRules.update { it - noteId }
    }
    
    /**
     * Clear all suggestions
     */
    fun clearAllSuggestions() {
        _suggestedFolders.value = emptyMap()
        _relatedNotes.value = emptyMap()
        _generatedTemplates.value = emptyMap()
        _suggestedFolderRules.value = emptyMap()
        _smartFolderSuggestions.value = emptyList()
    }
    
    /**
     * Cancel any ongoing operations
     */
    fun cancelOperations() {
        currentAnalysisJob?.cancel()
        isProcessing = false
        _uiState.value = AISmartOrganizationUiState.Idle
    }
    
    override fun onCleared() {
        super.onCleared()
        currentAnalysisJob?.cancel()
    }
}

/**
 * UI state for AI Smart Organization features
 */
sealed class AISmartOrganizationUiState {
    object Idle : AISmartOrganizationUiState()
    data class Loading(val message: String = "Processing...") : AISmartOrganizationUiState()
    object Success : AISmartOrganizationUiState()
    data class Error(val message: String) : AISmartOrganizationUiState()
}
