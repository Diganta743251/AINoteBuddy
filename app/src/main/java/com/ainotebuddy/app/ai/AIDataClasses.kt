package com.ainotebuddy.app.ai

/**
 * Data classes for AI analysis results and related structures
 */

data class AIAnalysisResult(
    val sentiment: SentimentResult,
    val topics: List<String>,
    val entities: List<EntityResult>,
    val actionItems: List<ActionItem>,
    val keyPhrases: List<String>,
    val insights: List<String>,
    val contextualTags: List<String>,
    val confidence: Float
)

enum class EntityType {
    PERSON, ORGANIZATION, LOCATION, EMAIL, PHONE, DATE, TIME, URL, NUMBER, TASK, REMINDER, MONEY, MISC
}

// NOTE: SentimentResult and Sentiment are defined in SentimentAnalyzer.kt
// Use the classes from SentimentAnalyzer.kt without redeclaration

// Entities used by EntityRecognizer
data class EntityResult(
    val text: String,
    val type: EntityType,
    val confidence: Float,
    val importance: Float,
    val context: String
)

data class ActionItem(
    val text: String,
    val priority: ActionPriority,
    val dueDate: Long? = null,
    val confidence: Float = 0.0f,
    val context: String = ""
)

enum class ActionPriority {
    LOW, MEDIUM, HIGH, URGENT
}

// Minimal topic result used by search/voice features
data class TopicResult(
    val topic: String,
    val confidence: Float
)

// Time range enumeration
enum class TimeRange {
    LAST_HOUR,
    LAST_DAY,
    LAST_WEEK,
    LAST_MONTH,
    LAST_YEAR,
    ALL_TIME
}

// Batch operation enumeration
enum class BatchOperation {
    BULK_DELETE,
    BULK_EXPORT,
    BULK_TAG,
    BULK_CATEGORIZE,
    BULK_BACKUP,
    BULK_IMPORT,
    BULK_SYNC
}