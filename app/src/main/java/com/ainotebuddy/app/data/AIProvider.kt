package com.ainotebuddy.app.data

enum class AIProvider(val displayName: String, val description: String) {
    OPENAI("OpenAI", "GPT-3.5/4 - Most versatile, great for general tasks"),
    GEMINI("Google Gemini", "Gemini Pro - Excellent for analysis and reasoning"),
    CLAUDE("Anthropic Claude", "Claude 3 - Best for writing and creative tasks"),
    OFFLINE("Offline Mode", "Basic AI features without internet connection")
}

enum class ConnectionStatus {
    SUCCESS,
    FAILED
}

data class AISettings(
    val provider: AIProvider = AIProvider.OFFLINE,
    val apiKeys: Map<AIProvider, String> = emptyMap(),
    val selectedModel: String = "",
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val autoEnhanceEnabled: Boolean = true,
    val voiceProcessingEnabled: Boolean = true,
    val smartCategorizationEnabled: Boolean = true
)

data class AISettingsUiState(
    val selectedProvider: AIProvider = AIProvider.OFFLINE,
    val apiKeys: Map<AIProvider, String> = emptyMap(),
    val selectedModel: String = "",
    val availableModels: List<String> = emptyList(),
    val temperature: Float = 0.7f,
    val maxTokens: Int = 1000,
    val autoEnhanceEnabled: Boolean = true,
    val voiceProcessingEnabled: Boolean = true,
    val smartCategorizationEnabled: Boolean = true,
    val isTestingConnection: Boolean = false,
    val connectionStatus: ConnectionStatus? = null,
    val showConnectionResult: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: String = "Never"
)