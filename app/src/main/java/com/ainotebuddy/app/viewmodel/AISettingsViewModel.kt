package com.ainotebuddy.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.AIProvider
import com.ainotebuddy.app.data.ConnectionStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AISettingsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(AISettingsUiState())
    val uiState: StateFlow<AISettingsUiState> = _uiState.asStateFlow()

    // New API for aisettingsActivity
    private fun modelsFor(provider: AIProvider): List<String> = when (provider) {
        AIProvider.OPENAI -> listOf("gpt-4o", "gpt-4o-mini", "gpt-3.5-turbo")
        AIProvider.GEMINI -> listOf("gemini-1.5-pro", "gemini-1.5-flash")
        AIProvider.CLAUDE -> listOf("claude-3-5-sonnet", "claude-3-haiku")
        AIProvider.OFFLINE -> emptyList()
    }

    fun setAIProvider(provider: AIProvider) {
        viewModelScope.launch {
            val models = modelsFor(provider)
            _uiState.value = _uiState.value.copy(
                selectedProvider = provider,
                availableModels = models,
                selectedModel = models.firstOrNull().orEmpty()
            )
        }
    }

    fun setApiKey(provider: AIProvider, key: String) {
        viewModelScope.launch {
            val map = _uiState.value.apiKeys.toMutableMap()
            map[provider] = key
            _uiState.value = _uiState.value.copy(apiKeys = map)
        }
    }

    fun setModel(model: String) {
        _uiState.value = _uiState.value.copy(selectedModel = model)
    }

    fun setTemperature(value: Float) {
        _uiState.value = _uiState.value.copy(temperature = value.coerceIn(0f, 1f))
    }

    fun setMaxTokens(tokens: Int) {
        _uiState.value = _uiState.value.copy(maxTokens = tokens.coerceIn(100, 4000))
    }

    fun toggleAutoEnhance() {
        _uiState.value = _uiState.value.copy(autoEnhanceEnabled = !_uiState.value.autoEnhanceEnabled)
    }

    fun toggleVoiceProcessing() {
        _uiState.value = _uiState.value.copy(voiceProcessingEnabled = !_uiState.value.voiceProcessingEnabled)
    }

    fun toggleSmartCategorization() {
        _uiState.value = _uiState.value.copy(smartCategorizationEnabled = !_uiState.value.smartCategorizationEnabled)
    }

    fun resetApiKeys() {
        _uiState.value = _uiState.value.copy(apiKeys = emptyMap())
    }

    fun resetAllSettings() {
        _uiState.value = AISettingsUiState()
    }

    fun testConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingConnection = true, connectionStatus = null)
            // Simulate a successful test
            _uiState.value = _uiState.value.copy(
                isTestingConnection = false,
                connectionStatus = ConnectionStatus.SUCCESS,
                showConnectionResult = true
            )
        }
    }

    fun dismissConnectionResult() {
        _uiState.value = _uiState.value.copy(showConnectionResult = false)
    }
}

// Data classes and enums
data class AISettingsUiState(
    val isLoading: Boolean = false,
    val selectedProvider: AIProvider = AIProvider.OPENAI,
    val availableModels: List<String> = emptyList(),
    val selectedModel: String = "",
    val apiKeys: Map<AIProvider, String> = emptyMap(),
    val temperature: Float = 0.5f,
    val maxTokens: Int = 2000,
    val autoEnhanceEnabled: Boolean = true,
    val voiceProcessingEnabled: Boolean = true,
    val smartCategorizationEnabled: Boolean = true,
    val usageCount: Int = 0,
    val lastUsed: String = "Never",
    val isTestingConnection: Boolean = false,
    val connectionStatus: ConnectionStatus? = null,
    val showConnectionResult: Boolean = false,
    val autoSuggestionsEnabled: Boolean = true,
    val sentimentAnalysisEnabled: Boolean = true,
    val aiProcessingLevel: AIProcessingLevel = AIProcessingLevel.BALANCED,
    val suggestionConfidenceThreshold: Float = 0.7f,
    val autoApplySuggestions: Boolean = false,
    val isTesting: Boolean = false,
    val testResult: AITestResult? = null,
    val error: String? = null
)

data class AITestResult(
    val success: Boolean,
    val processingTime: Long,
    val sentimentDetected: Boolean,
    val topicsExtracted: Boolean,
    val entitiesFound: Boolean,
    val confidence: Float,
    val message: String
)

enum class AIProcessingLevel(val displayName: String, val description: String) {
    FAST("Fast", "Quick analysis with basic features"),
    BALANCED("Balanced", "Good balance of speed and accuracy"),
    COMPREHENSIVE("Comprehensive", "Detailed analysis with all features"),
    CUSTOM("Custom", "User-defined processing settings")
}