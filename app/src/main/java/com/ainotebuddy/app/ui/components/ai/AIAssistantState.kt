package com.ainotebuddy.app.ui.components.ai

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.KeyboardVoice
import com.ainotebuddy.app.ai.AISuggestion
import com.ainotebuddy.app.ai.SentimentAnalysisResult
import com.ainotebuddy.app.ui.components.ai.AIAssistantTab

/**
 * State holder for the AI Assistant panel
 */
@Stable
class AIAssistantState(
    initialTab: AIAssistantTab = AIAssistantTab.SUGGESTIONS,
    isVisible: Boolean = false,
    isLoading: Boolean = false,
    suggestions: List<AISuggestion> = emptyList(),
    sentimentResult: SentimentAnalysisResult? = null,
    selectedSuggestionId: String? = null,
    isVoiceInputActive: Boolean = false,
    voiceCommandResult: String? = null
) {
    var currentTab by mutableStateOf(initialTab)
    var isVisible by mutableStateOf(isVisible)
    var isLoading by mutableStateOf(isLoading)
    var suggestions by mutableStateOf(suggestions)
    var sentimentResult by mutableStateOf(sentimentResult)
    var selectedSuggestionId by mutableStateOf(selectedSuggestionId)
    var isVoiceInputActive by mutableStateOf(isVoiceInputActive)
    var voiceCommandResult by mutableStateOf(voiceCommandResult)
    
    val selectedSuggestion: AISuggestion?
        get() = suggestions.find { it.id == selectedSuggestionId }
    
    fun show(tab: AIAssistantTab = currentTab) {
        currentTab = tab
        isVisible = true
    }
    
    fun hide() {
        isVisible = false
    }
    
    fun toggle(tab: AIAssistantTab = currentTab) {
        if (isVisible && currentTab == tab) {
            hide()
        } else {
            show(tab)
        }
    }
    
    fun updateSuggestions(newSuggestions: List<AISuggestion>) {
        suggestions = newSuggestions
        // Clear selection if the selected suggestion is no longer in the list
        if (selectedSuggestionId != null && newSuggestions.none { it.id == selectedSuggestionId }) {
            selectedSuggestionId = null
        }
    }
    
    fun selectSuggestion(suggestion: AISuggestion) {
        selectedSuggestionId = suggestion.id
    }
    
    fun clearSelection() {
        selectedSuggestionId = null
    }
    
    fun startVoiceInput() {
        isVoiceInputActive = true
        voiceCommandResult = null
    }
    
    fun stopVoiceInput() {
        isVoiceInputActive = false
    }
    
    fun updateVoiceCommandResult(result: String) {
        voiceCommandResult = result
    }
    
    fun clearVoiceCommandResult() {
        voiceCommandResult = null
    }
    
    fun updateSentimentResult(result: SentimentAnalysisResult) {
        sentimentResult = result
    }
    
    fun clearSentimentResult() {
        sentimentResult = null
    }
    
    fun reset() {
        currentTab = AIAssistantTab.SUGGESTIONS
        isVisible = false
        isLoading = false
        suggestions = emptyList()
        sentimentResult = null
        selectedSuggestionId = null
        isVoiceInputActive = false
        voiceCommandResult = null
    }
}

/**
 * Remember and manage the AI Assistant state
 */
@Composable
fun rememberAIAssistantState(
    initialTab: AIAssistantTab = AIAssistantTab.SUGGESTIONS,
    isVisible: Boolean = false,
    isLoading: Boolean = false,
    suggestions: List<AISuggestion> = emptyList(),
    sentimentResult: SentimentAnalysisResult? = null
): AIAssistantState {
    return remember {
        AIAssistantState(
            initialTab = initialTab,
            isVisible = isVisible,
            isLoading = isLoading,
            suggestions = suggestions,
            sentimentResult = sentimentResult
        )
    }
}

/**
 * Extension function to check if a tab is currently selected
 */
@Composable
fun AIAssistantState.isTabSelected(tab: AIAssistantTab): Boolean {
    return currentTab == tab
}

/**
 * Extension function to navigate to a specific tab
 */
fun AIAssistantState.navigateTo(tab: AIAssistantTab) {
    currentTab = tab
    isVisible = true
}

/**
 * Extension function to show loading state
 */
fun AIAssistantState.showLoading(show: Boolean = true) {
    isLoading = show
}

/**
 * Extension function to update the AI Assistant state with new suggestions
 */
fun AIAssistantState.updateWithSuggestions(newSuggestions: List<AISuggestion>) {
    updateSuggestions(newSuggestions)
    showLoading(false)
}

/**
 * Extension function to update the AI Assistant state with sentiment analysis result
 */
fun AIAssistantState.updateWithSentimentResult(result: SentimentAnalysisResult) {
    updateSentimentResult(result)
    showLoading(false)
}

/**
 * Extension function to handle voice command result
 */
fun AIAssistantState.handleVoiceCommandResult(result: String) {
    updateVoiceCommandResult(result)
    stopVoiceInput()
    showLoading(false)
}

/**
 * Extension function to get the current tab icon based on selection state
 */
@Composable
fun AIAssistantState.getTabIcon(tab: AIAssistantTab): ImageVector {
    return if (isTabSelected(tab)) {
        when (tab) {
            AIAssistantTab.SUGGESTIONS -> Icons.Filled.AutoAwesome
            AIAssistantTab.SENTIMENT -> Icons.Filled.SentimentSatisfied
            AIAssistantTab.TAGS -> Icons.Filled.Label
            AIAssistantTab.VOICE -> Icons.Filled.KeyboardVoice
        }
    } else {
        when (tab) {
            AIAssistantTab.SUGGESTIONS -> Icons.Outlined.AutoAwesome
            AIAssistantTab.SENTIMENT -> Icons.Outlined.SentimentSatisfied
            AIAssistantTab.TAGS -> Icons.Outlined.Label
            AIAssistantTab.VOICE -> Icons.Outlined.KeyboardVoice
        }
    }
}
