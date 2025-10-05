package com.ainotebuddy.app.viewmodel

import com.ainotebuddy.app.ai.ContextualAnalysis
import com.ainotebuddy.app.data.NoteEntity

/**
 * UI State classes for AI Insights
 */
sealed class AIInsightsUiState {
    object Loading : AIInsightsUiState()
    
    data class Success(
        val analysis: ContextualAnalysis,
        val relatedNotes: List<NoteEntity>
    ) : AIInsightsUiState()
    
    data class Error(
        val message: String
    ) : AIInsightsUiState()
}