package com.ainotebuddy.app.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive analytics utilities for user behavior tracking and insights
 */

@Singleton
class AnalyticsUtils @Inject constructor(
    private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics
) {
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private val timeFormatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    // User Engagement Analytics
    fun trackUserEngagement(
        sessionDuration: Long,
        notesCreated: Int,
        notesEdited: Int,
        searchesPerformed: Int
    ) {
        val bundle = Bundle().apply {
            putLong("session_duration", sessionDuration)
            putInt("notes_created", notesCreated)
            putInt("notes_edited", notesEdited)
            putInt("searches_performed", searchesPerformed)
            putDouble("engagement_score", calculateEngagementScore(sessionDuration, notesCreated, notesEdited, searchesPerformed))
        }
        
        firebaseAnalytics.logEvent("user_engagement", bundle)
    }
    
    // Note Analytics
    fun trackNoteCreation(
        noteLength: Int,
        hasImages: Boolean,
        hasVoiceRecording: Boolean,
        category: String?,
        timeToCreate: Long
    ) {
        val bundle = Bundle().apply {
            putInt("note_length", noteLength)
            putBoolean("has_images", hasImages)
            putBoolean("has_voice_recording", hasVoiceRecording)
            putString("category", category ?: "uncategorized")
            putLong("time_to_create", timeToCreate)
        }
        
        firebaseAnalytics.logEvent("note_created", bundle)
    }
    
    fun trackNoteEditing(
        originalLength: Int,
        finalLength: Int,
        editDuration: Long,
        changesCount: Int
    ) {
        val bundle = Bundle().apply {
            putInt("original_length", originalLength)
            putInt("final_length", finalLength)
            putLong("edit_duration", editDuration)
            putInt("changes_count", changesCount)
            putInt("length_change", finalLength - originalLength)
        }
        
        firebaseAnalytics.logEvent("note_edited", bundle)
    }
    
    // AI Feature Analytics
    fun trackAIFeatureUsage(
        featureType: AIFeatureType,
        success: Boolean,
        processingTime: Long,
        userSatisfaction: Float? = null
    ) {
        val bundle = Bundle().apply {
            putString("feature_type", featureType.name)
            putBoolean("success", success)
            putLong("processing_time", processingTime)
            userSatisfaction?.let { putFloat("user_satisfaction", it) }
        }
        
        firebaseAnalytics.logEvent("ai_feature_used", bundle)
    }
    
    // Voice Feature Analytics
    fun trackVoiceInteraction(
        interactionType: VoiceInteractionType,
        duration: Long,
        accuracy: Float,
        language: String
    ) {
        val bundle = Bundle().apply {
            putString("interaction_type", interactionType.name)
            putLong("duration", duration)
            putFloat("accuracy", accuracy)
            putString("language", language)
        }
        
        firebaseAnalytics.logEvent("voice_interaction", bundle)
    }
    
    // Search Analytics
    fun trackSearchBehavior(
        query: String,
        resultsCount: Int,
        timeToFirstResult: Long,
        filterUsed: String?,
        sortOrder: String?
    ) {
        val bundle = Bundle().apply {
            putString("query_length_category", categorizeQueryLength(query.length))
            putInt("results_count", resultsCount)
            putLong("time_to_first_result", timeToFirstResult)
            putString("filter_used", filterUsed ?: "none")
            putString("sort_order", sortOrder ?: "default")
            putBoolean("has_results", resultsCount > 0)
        }
        
        firebaseAnalytics.logEvent("search_performed", bundle)
    }
    
    // Performance Analytics
    fun trackPerformanceMetrics(metrics: PerformanceMetrics) {
        val bundle = Bundle().apply {
            putLong("app_startup_time", metrics.appStartupTime)
            putLong("database_query_time", metrics.databaseQueryTime)
            putLong("ui_render_time", metrics.uiRenderTime)
            putFloat("memory_usage", metrics.memoryUsage)
            putFloat("cpu_usage", metrics.cpuUsage)
            putInt("crash_count", metrics.crashCount)
        }
        
        firebaseAnalytics.logEvent("performance_metrics", bundle)
    }
    
    // Feature Adoption Analytics
    fun trackFeatureAdoption(
        featureName: String,
        isFirstUse: Boolean,
        usageFrequency: UsageFrequency
    ) {
        val bundle = Bundle().apply {
            putString("feature_name", featureName)
            putBoolean("is_first_use", isFirstUse)
            putString("usage_frequency", usageFrequency.name)
        }
        
        firebaseAnalytics.logEvent("feature_adoption", bundle)
    }
    
    // User Journey Analytics
    fun trackUserJourney(
        currentScreen: String,
        previousScreen: String?,
        timeOnScreen: Long,
        actionsTaken: List<String>
    ) {
        val bundle = Bundle().apply {
            putString("current_screen", currentScreen)
            putString("previous_screen", previousScreen ?: "none")
            putLong("time_on_screen", timeOnScreen)
            putString("actions_taken", actionsTaken.joinToString(","))
            putInt("actions_count", actionsTaken.size)
        }
        
        firebaseAnalytics.logEvent("user_journey", bundle)
    }
    
    // Content Analytics
    fun analyzeNoteContent(content: String): ContentAnalysis {
        val wordCount = content.split("\\s+".toRegex()).size
        val characterCount = content.length
        val sentenceCount = content.split("[.!?]+".toRegex()).size
        val hasLinks = content.contains("http")
        val hasEmails = content.contains("@")
        val hasPhoneNumbers = content.contains(Regex("\\d{3}-\\d{3}-\\d{4}"))
        
        return ContentAnalysis(
            wordCount = wordCount,
            characterCount = characterCount,
            sentenceCount = sentenceCount,
            averageWordsPerSentence = if (sentenceCount > 0) wordCount.toFloat() / sentenceCount else 0f,
            hasLinks = hasLinks,
            hasEmails = hasEmails,
            hasPhoneNumbers = hasPhoneNumbers,
            readingTimeMinutes = estimateReadingTime(wordCount),
            contentType = determineContentType(content)
        )
    }
    
    // Time-based Analytics
    fun getUsagePatternsByTimeRange(timeRange: TimeRange): Flow<UsagePatterns> = flow {
        val patterns = when (timeRange) {
            TimeRange.DAY -> getDailyUsagePatterns()
            TimeRange.WEEK -> getWeeklyUsagePatterns()
            TimeRange.MONTH -> getMonthlyUsagePatterns()
            TimeRange.YEAR -> getYearlyUsagePatterns()
            TimeRange.ALL_TIME -> getAllTimeUsagePatterns()
        }
        emit(patterns)
    }
    
    // Helper Functions
    private fun calculateEngagementScore(
        sessionDuration: Long,
        notesCreated: Int,
        notesEdited: Int,
        searchesPerformed: Int
    ): Double {
        val durationScore = minOf(sessionDuration / 60000.0, 1.0) // Max 1 point for 1+ minutes
        val activityScore = (notesCreated * 0.5 + notesEdited * 0.3 + searchesPerformed * 0.2)
        return (durationScore + activityScore).coerceAtMost(10.0)
    }
    
    private fun categorizeQueryLength(length: Int): String = when {
        length <= 5 -> "short"
        length <= 15 -> "medium"
        length <= 30 -> "long"
        else -> "very_long"
    }
    
    private fun estimateReadingTime(wordCount: Int): Int {
        return (wordCount / 200).coerceAtLeast(1) // Average reading speed: 200 words per minute
    }
    
    private fun determineContentType(content: String): ContentType {
        return when {
            content.contains(Regex("- \\[ \\]|\\* \\[ \\]")) -> ContentType.CHECKLIST
            content.contains(Regex("^#{1,6} ", RegexOption.MULTILINE)) -> ContentType.STRUCTURED
            content.contains(Regex("https?://")) -> ContentType.REFERENCE
            content.split("\\s+".toRegex()).size < 50 -> ContentType.BRIEF_NOTE
            content.split("\\s+".toRegex()).size > 500 -> ContentType.DETAILED_DOCUMENT
            else -> ContentType.GENERAL_NOTE
        }
    }
    
    private fun getDailyUsagePatterns(): UsagePatterns {
        // Implementation for daily patterns
        return UsagePatterns(
            timeRange = TimeRange.DAY,
            totalSessions = 0,
            averageSessionDuration = 0L,
            peakUsageHour = 0,
            mostUsedFeatures = emptyList(),
            engagementTrend = 0f
        )
    }
    
    private fun getWeeklyUsagePatterns(): UsagePatterns {
        // Implementation for weekly patterns
        return UsagePatterns(
            timeRange = TimeRange.WEEK,
            totalSessions = 0,
            averageSessionDuration = 0L,
            peakUsageHour = 0,
            mostUsedFeatures = emptyList(),
            engagementTrend = 0f
        )
    }
    
    private fun getMonthlyUsagePatterns(): UsagePatterns {
        // Implementation for monthly patterns
        return UsagePatterns(
            timeRange = TimeRange.MONTH,
            totalSessions = 0,
            averageSessionDuration = 0L,
            peakUsageHour = 0,
            mostUsedFeatures = emptyList(),
            engagementTrend = 0f
        )
    }
    
    private fun getYearlyUsagePatterns(): UsagePatterns {
        // Implementation for yearly patterns
        return UsagePatterns(
            timeRange = TimeRange.YEAR,
            totalSessions = 0,
            averageSessionDuration = 0L,
            peakUsageHour = 0,
            mostUsedFeatures = emptyList(),
            engagementTrend = 0f
        )
    }
    
    private fun getAllTimeUsagePatterns(): UsagePatterns {
        // Implementation for all-time patterns
        return UsagePatterns(
            timeRange = TimeRange.ALL_TIME,
            totalSessions = 0,
            averageSessionDuration = 0L,
            peakUsageHour = 0,
            mostUsedFeatures = emptyList(),
            engagementTrend = 0f
        )
    }
}

// Data Classes
@Serializable
data class ContentAnalysis(
    val wordCount: Int,
    val characterCount: Int,
    val sentenceCount: Int,
    val averageWordsPerSentence: Float,
    val hasLinks: Boolean,
    val hasEmails: Boolean,
    val hasPhoneNumbers: Boolean,
    val readingTimeMinutes: Int,
    val contentType: ContentType
)

@Serializable
data class PerformanceMetrics(
    val appStartupTime: Long,
    val databaseQueryTime: Long,
    val uiRenderTime: Long,
    val memoryUsage: Float,
    val cpuUsage: Float,
    val crashCount: Int
)

@Serializable
data class UsagePatterns(
    val timeRange: TimeRange,
    val totalSessions: Int,
    val averageSessionDuration: Long,
    val peakUsageHour: Int,
    val mostUsedFeatures: List<String>,
    val engagementTrend: Float
)

// Enums
enum class AIFeatureType {
    TEXT_ANALYSIS,
    SENTIMENT_DETECTION,
    KEYWORD_EXTRACTION,
    CONTENT_SUGGESTION,
    AUTO_CATEGORIZATION,
    SMART_SEARCH
}

enum class VoiceInteractionType {
    DICTATION,
    COMMAND,
    SEARCH,
    PLAYBACK
}

enum class UsageFrequency {
    FIRST_TIME,
    OCCASIONAL,
    REGULAR,
    FREQUENT,
    POWER_USER
}

enum class ContentType {
    BRIEF_NOTE,
    DETAILED_DOCUMENT,
    CHECKLIST,
    STRUCTURED,
    REFERENCE,
    GENERAL_NOTE
}

enum class TimeRange {
    DAY,
    WEEK,
    MONTH,
    YEAR,
    ALL_TIME
}