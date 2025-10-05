package com.ainotebuddy.app.ui.dashboard.widgets

import com.ainotebuddy.app.ai.*
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.integration.ContentInsights

// Provide missing conveniences so EnhancedAISuggestionsWidget compiles with current minimal AI layer

// Map NoteEntity.updatedAt to legacy lastModified used by UI
val NoteEntity.lastModified: Long
    get() = this.updatedAt

// Lightweight types to satisfy UI helpers
data class SentimentTrend(
    val positiveCount: Int = 0,
    val neutralCount: Int = 0,
    val negativeCount: Int = 0,
    val averagePolarity: Float = 0f
)

data class TopicFrequency(val topic: String, val frequency: Int)

data class ActionItemStats(
    val totalActionItems: Int,
    val highPriorityCount: Int,
    val withDueDates: Int
)

data class WritingPatterns(
    val averageNoteLength: Float,
    val peakWritingHour: Int,
    val writingFrequency: Float
)

data class EntityFrequency(val entity: String, val frequency: Int, val type: EntityType)

// Extension properties to supply richer fields for ContentInsights used by UI
val ContentInsights.sentimentTrend: SentimentTrend
    get() = SentimentTrend()

val ContentInsights.topTopics: List<TopicFrequency>
    get() = emptyList()

val ContentInsights.actionItemStats: ActionItemStats
    get() = ActionItemStats(totalActionItems = 0, highPriorityCount = 0, withDueDates = 0)

val ContentInsights.writingPatterns: WritingPatterns
    get() = WritingPatterns(averageNoteLength = 0f, peakWritingHour = 12, writingFrequency = 0f)

val ContentInsights.topEntities: List<EntityFrequency>
    get() = emptyList()

// Minimal implementations referenced by the widget
fun AIAnalysisEngine.generateContentInsights(notes: List<NoteEntity>): ContentInsights {
    val key = "Notes analyzed: ${notes.size}"
    val sentiment = "Neutral"
    return ContentInsights(keyInsights = listOf(key), sentimentSummary = sentiment)
}

fun AIAnalysisEngine.generateSmartSuggestions(
    note: NoteEntity,
    analysis: AIAnalysisResult,
    notes: List<NoteEntity>
): List<SmartSuggestion> {
    val suggestions = mutableListOf<SmartSuggestion>()

    // Suggest adding tags if none
    if (note.tags.isBlank()) {
        suggestions += SmartSuggestion(
            id = "add_tags_${note.id}",
            type = SuggestionType.TAGGING,
            title = "Add tags",
            description = "Tag this note to organize it better",
            confidence = 0.7f
        )
    }

    // Suggest organization for long notes
    val isLong = note.wordCount > 300
    if (isLong) {
        suggestions += SmartSuggestion(
            id = "organize_${note.id}",
            type = SuggestionType.ORGANIZATION,
            title = "Organize long note",
            description = "Consider splitting into sections or adding headings",
            confidence = 0.6f
        )
    }

    // Generic content improvement suggestion
    suggestions += SmartSuggestion(
        id = "improve_${note.id}",
        type = SuggestionType.CONTENT_IMPROVEMENT,
        title = "Improve clarity",
        description = "Review for concise language and key points",
        confidence = 0.5f
    )

    return suggestions
}
