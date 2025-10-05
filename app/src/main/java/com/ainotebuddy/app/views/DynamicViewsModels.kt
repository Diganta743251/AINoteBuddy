package com.ainotebuddy.app.views

import com.ainotebuddy.app.data.NoteEntity

/**
 * Data models for Dynamic Smart Views system
 */

/**
 * Represents a smart view that intelligently organizes notes
 */
data class SmartView(
    val id: String,
    val title: String,
    val subtitle: String,
    val notes: List<NoteEntity>,
    val viewType: ViewType,
    val priority: ViewPriority,
    val icon: String,
    val metadata: Map<String, String> = emptyMap(),
    val isCustomized: Boolean = false,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Types of smart views available
 */
enum class ViewType {
    TIME_BASED,
    TOPIC_BASED,
    PRIORITY_BASED,
    PROJECT_BASED,
    SENTIMENT_BASED,
    CUSTOM
}

/**
 * Priority levels for views
 */
enum class ViewPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
}

/**
 * Organization paradigms for intelligent switching
 */
enum class OrganizationParadigm {
    TIME_BASED,
    TOPIC_BASED,
    PRIORITY_BASED,
    PROJECT_BASED,
    SENTIMENT_BASED,
    INTELLIGENT_AUTO
}

/**
 * AI suggestion for new views
 */
data class ViewSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val paradigm: OrganizationParadigm,
    val confidence: Float,
    val reason: String,
    val estimatedNoteCount: Int = 0
)

/**
 * User preferences for view organization
 */
data class UserViewPreferences(
    val preferredParadigm: OrganizationParadigm = OrganizationParadigm.INTELLIGENT_AUTO,
    val maxViewsPerScreen: Int = 6,
    val autoAcceptHighConfidenceSuggestions: Boolean = false,
    val preferredViewTypes: Set<ViewType> = setOf(
        ViewType.TIME_BASED,
        ViewType.TOPIC_BASED,
        ViewType.PRIORITY_BASED
    ),
    val customViewConfigurations: Map<String, ViewCustomization> = emptyMap()
)

/**
 * Customization options for views
 */
data class ViewCustomization(
    val title: String? = null,
    val icon: String? = null,
    val priority: ViewPriority? = null,
    val sortOrder: SortOrder? = null,
    val filterCriteria: FilterCriteria? = null
)

/**
 * Sort order options for views
 */
enum class SortOrder {
    NEWEST_FIRST,
    OLDEST_FIRST,
    ALPHABETICAL,
    PRIORITY,
    RELEVANCE
}

/**
 * Filter criteria for customizing views
 */
data class FilterCriteria(
    val dateRange: DateRange? = null,
    val keywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val hasAttachments: Boolean? = null
)

/**
 * Date range for filtering
 */
data class DateRange(
    val startDate: Long,
    val endDate: Long
)

/**
 * Note priority levels for priority-based organization
 */
enum class NotePriority {
    URGENT_IMPORTANT,
    IMPORTANT_NOT_URGENT,
    URGENT_NOT_IMPORTANT,
    NOT_URGENT_NOT_IMPORTANT
}

/**
 * Note sentiment for sentiment-based organization
 */
enum class NoteSentiment {
    POSITIVE,
    NEUTRAL,
    NEGATIVE
}

/**
 * Topic information from AI analysis
 */
data class TopicInfo(
    val id: String,
    val name: String,
    val keywords: List<String>,
    val confidence: Float,
    val suggestedIcon: String? = null
)

/**
 * Project information from AI analysis
 */
data class ProjectInfo(
    val id: String,
    val name: String,
    val status: String,
    val progress: Float,
    val estimatedCompletion: Long? = null
)

/**
 * Analysis results for optimal organization
 */
data class OrganizationAnalysis(
    val hasStrongTemporalPatterns: Boolean,
    val hasDistinctTopics: Boolean,
    val hasUrgentItems: Boolean,
    val hasProjectStructure: Boolean,
    val hasEmotionalVariance: Boolean,
    val recommendedParadigm: OrganizationParadigm,
    val confidence: Float
)

/**
 * Topic modeling results
 */
data class TopicModelingResult(
    val topics: List<TopicInfo>,
    val noteTopicAssignments: Map<String, List<String>>, // noteId -> topicIds
    val overallCoherence: Float
)

/**
 * Priority analysis results
 */
data class PriorityAnalysisResult(
    val notePriorities: Map<String, NotePriority>, // noteId -> priority
    val urgentCount: Int,
    val importantCount: Int
) {
    fun getPriority(noteId: String): NotePriority {
        return notePriorities[noteId] ?: NotePriority.NOT_URGENT_NOT_IMPORTANT
    }
}

/**
 * Project identification results
 */
data class ProjectAnalysisResult(
    val projects: List<ProjectInfo>,
    val noteProjectAssignments: Map<String, String> // noteId -> projectId
)

/**
 * Sentiment analysis results
 */
data class SentimentAnalysisResult(
    val noteSentiments: Map<String, NoteSentiment>, // noteId -> sentiment
    val overallSentiment: NoteSentiment,
    val sentimentDistribution: Map<NoteSentiment, Int>
) {
    fun getSentiment(noteId: String): NoteSentiment {
        return noteSentiments[noteId] ?: NoteSentiment.NEUTRAL
    }
}

/**
 * View interaction analytics
 */
data class ViewInteractionData(
    val viewId: String,
    val viewType: ViewType,
    val clickCount: Int,
    val timeSpent: Long, // milliseconds
    val notesOpened: Int,
    val lastInteraction: Long
)

/**
 * Smart view configuration for persistence
 */
data class SmartViewConfig(
    val id: String,
    val title: String,
    val viewType: ViewType,
    val paradigm: OrganizationParadigm,
    val customization: ViewCustomization?,
    val isEnabled: Boolean = true,
    val displayOrder: Int = 0
)
