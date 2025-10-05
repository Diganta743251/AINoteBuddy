package com.ainotebuddy.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.ai.AIContextualService
import com.ainotebuddy.app.ai.ContextualAnalysis
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AIInsightsViewModel @Inject constructor(
    private val aiContextualService: AIContextualService,
    private val noteRepository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<AIInsightsUiState>(AIInsightsUiState.Loading)
    val uiState: StateFlow<AIInsightsUiState> = _uiState.asStateFlow()

    fun loadInsights(noteId: Long) {
        viewModelScope.launch {
            try {
                _uiState.value = AIInsightsUiState.Loading
                
                val note = noteRepository.getNoteById(noteId)
                if (note == null) {
                    _uiState.value = AIInsightsUiState.Error("Note not found")
                    return@launch
                }
                
                val analysis = aiContextualService.analyzeContextualRelevance(note)
                val relatedNotes = if (analysis.relatedNotes.isNotEmpty()) {
                    analysis.relatedNotes.mapNotNull { relatedId ->
                        noteRepository.getNoteById(relatedId)
                    }
                } else {
                    emptyList()
                }
                
                _uiState.value = AIInsightsUiState.Success(
                    analysis = analysis,
                    relatedNotes = relatedNotes
                )
            } catch (e: Exception) {
                _uiState.value = AIInsightsUiState.Error(
                    e.message ?: "Failed to load AI insights"
                )
            }
        }
    }
    
    fun navigateToNote(noteId: Long) {
        // Navigation logic would be handled by the composable
        // This could trigger a navigation event
    }
}

// State sealed class is defined in AIInsightsUiState.kt (single source of truth)