package com.ainotebuddy.app.util

import android.app.Activity
import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.Serializable
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Comprehensive analytics events system for detailed user behavior tracking
 */

@Singleton
class AnalyticsEvents @Inject constructor(
    private val context: Context,
    private val firebaseAnalytics: FirebaseAnalytics
) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val eventQueue = mutableListOf<QueuedEvent>()
    private val maxQueueSize = 100
    
    // User Journey Events
    fun trackAppLaunch(coldStart: Boolean, launchTime: Long) {
        logEvent("app_launch") {
            putBoolean("cold_start", coldStart)
            putLong("launch_time_ms", launchTime)
            putString("session_id", generateSessionId())
        }
    }
    
    fun trackScreenView(screenName: String, timeSpent: Long? = null, previousScreen: String? = null) {
        logEvent("screen_view") {
            putString("screen_name", screenName)
            putString("screen_class", screenName)
            timeSpent?.let { putLong("time_spent_ms", it) }
            previousScreen?.let { putString("previous_screen", it) }
        }
    }
    
    fun trackUserAction(
        action: UserAction,
        context: String? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        logEvent("user_action") {
            putString("action_type", action.name)
            context?.let { putString("action_context", it) }
            metadata.forEach { (key, value) ->
                when (value) {
                    is String -> putString("meta_$key", value)
                    is Int -> putInt("meta_$key", value)
                    is Long -> putLong("meta_$key", value)
                    is Float -> putFloat("meta_$key", value)
                    is Boolean -> putBoolean("meta_$key", value)
                }
            }
        }
    }
    
    // Note Management Events
    fun trackNoteCreated(
        noteId: Long,
        method: NoteCreationMethod,
        hasTitle: Boolean,
        contentLength: Int,
        hasImages: Boolean = false,
        hasVoice: Boolean = false,
        timeToCreate: Long? = null
    ) {
        logEvent("note_created") {
            putLong("note_id", noteId)
            putString("creation_method", method.name)
            putBoolean("has_title", hasTitle)
            putInt("content_length", contentLength)
            putBoolean("has_images", hasImages)
            putBoolean("has_voice", hasVoice)
            timeToCreate?.let { putLong("time_to_create_ms", it) }
            putString("content_category", categorizeContentLength(contentLength))
        }
    }
    
    fun trackNoteEdited(
        noteId: Long,
        editType: EditType,
        oldLength: Int,
        newLength: Int,
        editDuration: Long,
        changesCount: Int = 1
    ) {
        logEvent("note_edited") {
            putLong("note_id", noteId)
            putString("edit_type", editType.name)
            putInt("old_length", oldLength)
            putInt("new_length", newLength)
            putInt("length_change", newLength - oldLength)
            putLong("edit_duration_ms", editDuration)
            putInt("changes_count", changesCount)
            putString("edit_magnitude", categorizeEditMagnitude(oldLength, newLength))
        }
    }
    
    fun trackNoteDeleted(
        noteId: Long,
        age: Long,
        contentLength: Int,
        confirmationRequired: Boolean = true
    ) {
        logEvent("note_deleted") {
            putLong("note_id", noteId)
            putLong("note_age_ms", age)
            putInt("content_length", contentLength)
            putBoolean("confirmation_required", confirmationRequired)
            putString("age_category", categorizeNoteAge(age))
        }
    }
    
    // Search and Discovery Events
    fun trackSearch(
        query: String,
        resultsCount: Int,
        searchType: SearchType,
        filters: List<String> = emptyList(),
        timeToResults: Long,
        resultClicked: Boolean = false
    ) {
        logEvent("search_performed") {
            putString("query_hash", hashQuery(query)) // Privacy-safe query tracking
            putInt("query_length", query.length)
            putInt("results_count", resultsCount)
            putString("search_type", searchType.name)
            putString("filters_used", filters.joinToString(","))
            putLong("time_to_results_ms", timeToResults)
            putBoolean("result_clicked", resultClicked)
            putString("query_category", categorizeQuery(query))
        }
    }
    
    fun trackSearchResultClicked(
        query: String,
        resultPosition: Int,
        resultType: String,
        noteId: Long
    ) {
        logEvent("search_result_clicked") {
            putString("query_hash", hashQuery(query))
            putInt("result_position", resultPosition)
            putString("result_type", resultType)
            putLong("clicked_note_id", noteId)
        }
    }
    
    // AI Feature Events
    fun trackAIFeatureUsed(
        feature: AIFeature,
        success: Boolean,
        processingTime: Long,
        confidence: Float? = null,
        noteId: Long? = null
    ) {
        logEvent("ai_feature_used") {
            putString("ai_feature", feature.name)
            putBoolean("success", success)
            putLong("processing_time_ms", processingTime)
            confidence?.let { putFloat("confidence_score", it) }
            noteId?.let { putLong("target_note_id", it) }
            putString("performance_category", categorizeAIPerformance(processingTime))
        }
    }
    
    fun trackAISuggestionShown(
        suggestionType: SuggestionType,
        noteId: Long,
        confidence: Float,
        position: Int = 0
    ) {
        logEvent("ai_suggestion_shown") {
            putString("suggestion_type", suggestionType.name)
            putLong("note_id", noteId)
            putFloat("confidence", confidence)
            putInt("position", position)
        }
    }
    
    fun trackAISuggestionAccepted(
        suggestionType: SuggestionType,
        noteId: Long,
        confidence: Float,
        timeToDecision: Long
    ) {
        logEvent("ai_suggestion_accepted") {
            putString("suggestion_type", suggestionType.name)
            putLong("note_id", noteId)
            putFloat("confidence", confidence)
            putLong("time_to_decision_ms", timeToDecision)
        }
    }
    
    // Voice Features Events
    fun trackVoiceRecording(
        duration: Long,
        language: String,
        success: Boolean,
        transcriptionAccuracy: Float? = null
    ) {
        logEvent("voice_recording") {
            putLong("duration_ms", duration)
            putString("language", language)
            putBoolean("success", success)
            transcriptionAccuracy?.let { putFloat("transcription_accuracy", it) }
            putString("duration_category", categorizeVoiceDuration(duration))
        }
    }
    
    fun trackVoiceCommand(
        command: VoiceCommandType,
        success: Boolean,
        confidence: Float,
        executionTime: Long
    ) {
        logEvent("voice_command") {
            putString("command_type", command.name)
            putBoolean("success", success)
            putFloat("confidence", confidence)
            putLong("execution_time_ms", executionTime)
        }
    }
    
    // Productivity Events
    fun trackSessionProductivity(
        sessionDuration: Long,
        notesCreated: Int,
        notesEdited: Int,
        searchesPerformed: Int,
        aiInteractions: Int
    ) {
        logEvent("session_productivity") {
            putLong("session_duration_ms", sessionDuration)
            putInt("notes_created", notesCreated)
            putInt("notes_edited", notesEdited)
            putInt("searches_performed", searchesPerformed)
            putInt("ai_interactions", aiInteractions)
            putFloat("productivity_score", calculateProductivityScore(
                sessionDuration, notesCreated, notesEdited, searchesPerformed, aiInteractions
            ))
        }
    }
    
    fun trackGoalCompletion(
        goalType: ProductivityGoal,
        timeToComplete: Long,
        difficulty: GoalDifficulty
    ) {
        logEvent("goal_completed") {
            putString("goal_type", goalType.name)
            putLong("completion_time_ms", timeToComplete)
            putString("difficulty", difficulty.name)
        }
    }
    
    // Error and Performance Events
    fun trackError(
        error: AppError,
        context: String,
        severity: ErrorSeverity,
        stackTrace: String? = null
    ) {
        logEvent("app_error") {
            putString("error_type", error.name)
            putString("error_context", context)
            putString("severity", severity.name)
            putString("error_id", UUID.randomUUID().toString())
            // Don't log full stack trace for privacy, just error type
        }
    }
    
    fun trackPerformanceMetric(
        metric: PerformanceMetric,
        value: Double,
        threshold: Double? = null
    ) {
        logEvent("performance_metric") {
            putString("metric_name", metric.name)
            putDouble("metric_value", value)
            threshold?.let { 
                putDouble("threshold", it)
                putBoolean("exceeded_threshold", value > it)
            }
            putString("performance_level", categorizePerformance(metric, value))
        }
    }
    
    // Feature Usage Events
    fun trackFeatureDiscovery(
        feature: AppFeature,
        discoveryMethod: DiscoveryMethod,
        timeFromInstall: Long
    ) {
        logEvent("feature_discovered") {
            putString("feature_name", feature.name)
            putString("discovery_method", discoveryMethod.name)
            putLong("time_from_install_ms", timeFromInstall)
        }
    }
    
    fun trackFeatureFirstUse(
        feature: AppFeature,
        timeFromDiscovery: Long,
        helpUsed: Boolean = false
    ) {
        logEvent("feature_first_use") {
            putString("feature_name", feature.name)
            putLong("time_from_discovery_ms", timeFromDiscovery)
            putBoolean("help_used", helpUsed)
        }
    }
    
    // Settings and Preferences Events
    fun trackSettingChanged(
        setting: AppSetting,
        oldValue: String,
        newValue: String,
        changeReason: ChangeReason? = null
    ) {
        logEvent("setting_changed") {
            putString("setting_name", setting.name)
            putString("old_value_type", categorizeSettingValue(oldValue))
            putString("new_value_type", categorizeSettingValue(newValue))
            changeReason?.let { putString("change_reason", it.name) }
        }
    }
    
    // Export and Sharing Events
    fun trackNoteExport(
        format: ExportFormat,
        noteCount: Int,
        totalSize: Long,
        destination: ExportDestination
    ) {
        logEvent("note_export") {
            putString("export_format", format.name)
            putInt("note_count", noteCount)
            putLong("total_size_bytes", totalSize)
            putString("destination", destination.name)
        }
    }
    
    fun trackNoteShared(
        shareMethod: ShareMethod,
        noteId: Long,
        contentType: SharedContentType
    ) {
        logEvent("note_shared") {
            putString("share_method", shareMethod.name)
            putLong("note_id", noteId)
            putString("content_type", contentType.name)
        }
    }
    
    // Time-based Analytics Events
    fun trackTimeBasedUsage(timeRange: TimeRange) {
        scope.launch {
            val usageData = analyzeUsageInTimeRange(timeRange)
            
            logEvent("time_based_usage") {
                putString("time_range", timeRange.name)
                putInt("total_sessions", usageData.totalSessions)
                putLong("total_time_ms", usageData.totalTimeMs)
                putInt("notes_created", usageData.notesCreated)
                putFloat("engagement_score", usageData.engagementScore)
            }
        }
    }
    
    // Custom Events
    fun trackCustomEvent(
        eventName: String,
        parameters: Map<String, Any> = emptyMap()
    ) {
        logEvent(eventName) {
            parameters.forEach { (key, value) ->
                when (value) {
                    is String -> putString(key, value)
                    is Int -> putInt(key, value)
                    is Long -> putLong(key, value)
                    is Float -> putFloat(key, value)
                    is Double -> putDouble(key, value)
                    is Boolean -> putBoolean(key, value)
                }
            }
        }
    }
    
    // Batch Events
    fun trackBatchOperation(
        operation: BatchOperation,
        itemCount: Int,
        duration: Long,
        success: Boolean
    ) {
        logEvent("batch_operation") {
            putString("operation_type", operation.name)
            putInt("item_count", itemCount)
            putLong("duration_ms", duration)
            putBoolean("success", success)
            putFloat("items_per_second", itemCount.toFloat() / (duration / 1000f))
        }
    }
    
    // Helper functions
    private fun logEvent(eventName: String, parameterBuilder: Bundle.() -> Unit = {}) {
        val bundle = Bundle().apply(parameterBuilder)
        
        if (eventQueue.size >= maxQueueSize) {
            flushEvents()
        }
        
        val queuedEvent = QueuedEvent(
            name = eventName,
            parameters = bundle,
            timestamp = System.currentTimeMillis()
        )
        
        eventQueue.add(queuedEvent)
        
        // Log immediately for critical events
        if (isCriticalEvent(eventName)) {
            firebaseAnalytics.logEvent(eventName, bundle)
        }
    }
    
    private fun flushEvents() {
        scope.launch {
            eventQueue.forEach { event ->
                firebaseAnalytics.logEvent(event.name, event.parameters)
            }
            eventQueue.clear()
        }
    }
    
    private fun isCriticalEvent(eventName: String): Boolean {
        return eventName in listOf("app_error", "app_crash", "performance_issue")
    }
    
    private fun generateSessionId(): String {
        return "session_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
    }
    
    private fun hashQuery(query: String): String {
        return query.hashCode().toString()
    }
    
    private fun categorizeContentLength(length: Int): String = when {
        length == 0 -> "empty"
        length <= 50 -> "short"
        length <= 200 -> "medium"
        length <= 500 -> "long"
        else -> "very_long"
    }
    
    private fun categorizeEditMagnitude(oldLength: Int, newLength: Int): String {
        val change = kotlin.math.abs(newLength - oldLength)
        val percentChange = if (oldLength > 0) (change.toFloat() / oldLength) * 100 else 100f
        
        return when {
            percentChange < 10 -> "minor"
            percentChange < 50 -> "moderate"
            percentChange < 100 -> "major"
            else -> "complete_rewrite"
        }
    }
    
    private fun categorizeNoteAge(ageMs: Long): String {
        val hours = ageMs / (1000 * 60 * 60)
        return when {
            hours < 1 -> "fresh"
            hours < 24 -> "recent"
            hours < 168 -> "week_old" // 7 days
            hours < 720 -> "month_old" // 30 days
            else -> "old"
        }
    }
    
    private fun categorizeQuery(query: String): String = when {
        query.contains(Regex("\\d+")) -> "contains_numbers"
        query.contains("@") -> "contains_email"
        query.length <= 3 -> "very_short"
        query.length <= 10 -> "short"
        query.length <= 30 -> "medium"
        else -> "long"
    }
    
    private fun categorizeAIPerformance(timeMs: Long): String = when {
        timeMs < 500 -> "excellent"
        timeMs < 1000 -> "good"
        timeMs < 2000 -> "acceptable"
        timeMs < 5000 -> "slow"
        else -> "very_slow"
    }
    
    private fun categorizeVoiceDuration(durationMs: Long): String {
        val seconds = durationMs / 1000
        return when {
            seconds < 5 -> "very_short"
            seconds < 15 -> "short"
            seconds < 60 -> "medium"
            seconds < 300 -> "long"
            else -> "very_long"
        }
    }
    
    private fun calculateProductivityScore(
        sessionDuration: Long,
        notesCreated: Int,
        notesEdited: Int,
        searchesPerformed: Int,
        aiInteractions: Int
    ): Float {
        val minutesInSession = (sessionDuration / 60000f).coerceAtLeast(1f)
        val actionsPerMinute = (notesCreated + notesEdited + searchesPerformed + aiInteractions) / minutesInSession
        
        return (actionsPerMinute * 10).coerceAtMost(100f) // Scale to 0-100
    }
    
    private fun categorizePerformance(metric: PerformanceMetric, value: Double): String {
        return when (metric) {
            PerformanceMetric.APP_STARTUP_TIME -> when {
                value < 1000 -> "excellent"
                value < 2000 -> "good"
                value < 3000 -> "acceptable"
                else -> "poor"
            }
            PerformanceMetric.MEMORY_USAGE -> when {
                value < 50 -> "excellent"
                value < 100 -> "good"
                value < 200 -> "acceptable"
                else -> "poor"
            }
            PerformanceMetric.DATABASE_QUERY_TIME -> when {
                value < 100 -> "excellent"
                value < 500 -> "good"
                value < 1000 -> "acceptable"
                else -> "poor"
            }
            else -> "unknown"
        }
    }
    
    private fun categorizeSettingValue(value: String): String = when {
        value.toIntOrNull() != null -> "numeric"
        value.toBooleanStrictOrNull() != null -> "boolean"
        value.isBlank() -> "empty"
        value.length > 50 -> "long_text"
        else -> "short_text"
    }
    
    private suspend fun analyzeUsageInTimeRange(timeRange: TimeRange): UsageData {
        // Implementation would analyze actual usage data
        return UsageData(
            totalSessions = 0,
            totalTimeMs = 0L,
            notesCreated = 0,
            engagementScore = 0f
        )
    }
    
    fun cleanup() {
        flushEvents()
        scope.cancel()
    }
}

// Data classes and enums
data class QueuedEvent(
    val name: String,
    val parameters: Bundle,
    val timestamp: Long
)

data class UsageData(
    val totalSessions: Int,
    val totalTimeMs: Long,
    val notesCreated: Int,
    val engagementScore: Float
)

// Enums for event categorization
enum class UserAction {
    TAP, LONG_PRESS, SWIPE, PINCH_ZOOM, SCROLL, VOICE_COMMAND, KEYBOARD_SHORTCUT
}

enum class NoteCreationMethod {
    MANUAL_TEXT, VOICE_DICTATION, TEMPLATE, COPY_PASTE, IMPORT, AI_GENERATION
}

enum class EditType {
    TEXT_ADDITION, TEXT_DELETION, TEXT_REPLACEMENT, FORMATTING, TAG_ADDITION, TAG_REMOVAL, CATEGORY_CHANGE
}

enum class SearchType {
    QUICK_SEARCH, ADVANCED_SEARCH, VOICE_SEARCH, AI_SEARCH, FILTER_SEARCH
}

enum class AIFeature {
    SENTIMENT_ANALYSIS, TOPIC_EXTRACTION, SUMMARY_GENERATION, SMART_SUGGESTIONS, AUTO_CATEGORIZATION, CONTENT_ENHANCEMENT
}

enum class SuggestionType {
    CONTENT_IMPROVEMENT, TAGGING, ORGANIZATION, RELATED_NOTE, PRODUCTIVITY, FORMATTING
}

enum class VoiceCommandType {
    CREATE_NOTE, EDIT_NOTE, SEARCH, NAVIGATE, SETTINGS, PLAYBACK
}

enum class ProductivityGoal {
    DAILY_NOTE_TARGET, WEEKLY_WRITING_GOAL, ORGANIZATION_MILESTONE, SEARCH_EFFICIENCY
}

enum class GoalDifficulty {
    EASY, MEDIUM, HARD, EXPERT
}

enum class AppError {
    NETWORK_ERROR, DATABASE_ERROR, PERMISSION_ERROR, PARSING_ERROR, AI_SERVICE_ERROR, VOICE_ERROR
}

enum class ErrorSeverity {
    LOW, MEDIUM, HIGH, CRITICAL
}

enum class PerformanceMetric {
    APP_STARTUP_TIME, MEMORY_USAGE, DATABASE_QUERY_TIME, UI_RENDER_TIME, NETWORK_LATENCY
}

enum class AppFeature {
    VOICE_NOTES, AI_INSIGHTS, SMART_SEARCH, TEMPLATES, CATEGORIES, TAGS, EXPORT, SHARING
}

enum class DiscoveryMethod {
    TUTORIAL, EXPLORATION, TOOLTIP, NOTIFICATION, SEARCH, HELP_SECTION
}

enum class AppSetting {
    THEME, FONT_SIZE, LANGUAGE, NOTIFICATIONS, VOICE_LANGUAGE, AI_FEATURES, BACKUP_FREQUENCY
}

enum class ChangeReason {
    USER_PREFERENCE, ACCESSIBILITY, PERFORMANCE, RECOMMENDATION, DEFAULT_RESTORE
}

enum class ExportFormat {
    PDF, TEXT, MARKDOWN, JSON, CSV, HTML
}

enum class ExportDestination {
    LOCAL_STORAGE, CLOUD_DRIVE, EMAIL, SHARE_INTENT
}

enum class ShareMethod {
    SOCIAL_MEDIA, EMAIL, MESSAGE, CLIPBOARD, QR_CODE
}

enum class SharedContentType {
    FULL_NOTE, NOTE_SUMMARY, NOTE_LINK, NOTE_EXCERPT
}

enum class BatchOperation {
    BULK_DELETE, BULK_EXPORT, BULK_TAG, BULK_CATEGORIZE, BULK_BACKUP
}