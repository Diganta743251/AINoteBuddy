package com.ainotebuddy.app.ai

/**
 * AI Suggestion data classes
 */
data class AISuggestion(
    val id: String,
    val type: SuggestionType,
    val title: String,
    val description: String,
    val confidence: Float,
    val metadata: Map<String, Any> = emptyMap()
)

enum class SuggestionType {
    CONTENT_IMPROVEMENT,
    TAGGING,
    ORGANIZATION,
    RELATED_NOTE,
    PRODUCTIVITY,
    FORMATTING
}

data class EnhancedSearchQuery(
    val originalQuery: String,
    val enhancedTerms: List<String>,
    val filters: Map<String, String>,
    val confidence: Float
)