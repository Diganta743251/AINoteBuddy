package com.ainotebuddy.app.ai

/**
 * Enum representing available AI service providers
 */
enum class AIProvider(val displayName: String, val requiresApiKey: Boolean) {
    OPENAI("OpenAI GPT", true),
    GEMINI("Google Gemini", true),
    CLAUDE("Anthropic Claude", true),
    LOCAL("Local AI", false)
}