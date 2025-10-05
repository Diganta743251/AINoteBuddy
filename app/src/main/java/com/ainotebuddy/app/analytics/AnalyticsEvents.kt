package com.ainotebuddy.app.analytics

import android.os.Bundle
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.util.TimeRange
import com.ainotebuddy.app.data.repository.ExportFormat

/**
 * Analytics event constants and utilities for tracking user interactions
 */
object AnalyticsEvents {
    // Screen names
    const val SCREEN_ANALYTICS = "analytics_screen"
    const val SCREEN_TAG_ANALYTICS = "tag_analytics_screen"
    const val SCREEN_ACTIVITY_HEATMAP = "activity_heatmap_screen"
    
    // Event names
    const val EVENT_TIME_RANGE_SELECTED = "time_range_selected"
    const val EVENT_REFRESH_DATA = "refresh_data"
    const val EVENT_EXPORT_REPORT = "export_report"
    const val EVENT_TAG_SELECTED = "tag_selected"
    const val EVENT_ACTIVITY_DRILLDOWN = "activity_drilldown"
    const val EVENT_REPORT_GENERATED = "report_generated"
    const val EVENT_REPORT_EXPORTED = "report_exported"
    const val EVENT_ERROR = "analytics_error"
    
    // Parameter names
    const val PARAM_TIME_RANGE = "time_range"
    const val PARAM_TAG_NAME = "tag_name"
    const val PARAM_TAG_COUNT = "tag_count"
    const val PARAM_ACTIVITY_TYPE = "activity_type"
    const val PARAM_ACTIVITY_COUNT = "activity_count"
    const val PARAM_REPORT_FORMAT = "report_format"
    const val PARAM_REPORT_SIZE_KB = "report_size_kb"
    const val PARAM_GENERATION_TIME_MS = "generation_time_ms"
    const val PARAM_ERROR_MESSAGE = "error_message"
    const val PARAM_STACK_TRACE = "stack_trace"
    
    // Parameter values
    const val VALUE_TIME_RANGE_WEEK = "week"
    const val VALUE_TIME_RANGE_MONTH = "month"
    const val VALUE_TIME_RANGE_QUARTER = "quarter"
    const val VALUE_TIME_RANGE_YEAR = "year"
    const val VALUE_TIME_RANGE_ALL_TIME = "all_time"
    
    /**
     * Track when a time range is selected
     */
    fun logTimeRangeSelected(timeRange: TimeRange) {
        val params = Bundle().apply {
            putString(PARAM_TIME_RANGE, timeRange.toAnalyticsValue())
        }
        logEvent(EVENT_TIME_RANGE_SELECTED, params)
    }
    
    /**
     * Track when data is refreshed
     */
    fun logRefreshData() {
        logEvent(EVENT_REFRESH_DATA)
    }
    
    /**
     * Track when a report export is initiated
     */
    fun logExportReport(format: ExportFormat) {
        val params = Bundle().apply {
            putString(PARAM_REPORT_FORMAT, format.name.lowercase())
        }
        logEvent(EVENT_EXPORT_REPORT, params)
    }
    
    /**
     * Track when a tag is selected
     */
    fun logTagSelected(tag: String, count: Int) {
        val params = Bundle().apply {
            putString(PARAM_TAG_NAME, tag)
            putInt(PARAM_TAG_COUNT, count)
        }
        logEvent(EVENT_TAG_SELECTED, params)
    }
    
    /**
     * Track when drilling down into activity details
     */
    fun logActivityDrilldown(activityType: ActivityType, count: Int) {
        val params = Bundle().apply {
            putString(PARAM_ACTIVITY_TYPE, activityType.name.lowercase())
            putInt(PARAM_ACTIVITY_COUNT, count)
        }
        logEvent(EVENT_ACTIVITY_DRILLDOWN, params)
    }
    
    /**
     * Track when a report is successfully generated
     */
    fun logReportGenerated(
        format: ExportFormat,
        sizeBytes: Long,
        generationTimeMs: Long
    ) {
        val params = Bundle().apply {
            putString(PARAM_REPORT_FORMAT, format.name.lowercase())
            putLong(PARAM_REPORT_SIZE_KB, sizeBytes / 1024)
            putLong(PARAM_GENERATION_TIME_MS, generationTimeMs)
        }
        logEvent(EVENT_REPORT_GENERATED, params)
    }
    
    /**
     * Track when a report is successfully exported
     */
    fun logReportExported(
        format: ExportFormat,
        sizeBytes: Long,
        filePath: String
    ) {
        val params = Bundle().apply {
            putString(PARAM_REPORT_FORMAT, format.name.lowercase())
            putLong(PARAM_REPORT_SIZE_KB, sizeBytes / 1024)
            putString("file_path", filePath)
        }
        logEvent(EVENT_REPORT_EXPORTED, params)
    }
    
    /**
     * Track analytics-related errors
     */
    fun logError(
        errorMessage: String,
        stackTrace: String? = null,
        additionalParams: Map<String, Any>? = null
    ) {
        val params = Bundle().apply {
            putString(PARAM_ERROR_MESSAGE, errorMessage)
            stackTrace?.let { putString(PARAM_STACK_TRACE, it) }
            additionalParams?.forEach { (key, value) ->
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
        logEvent(EVENT_ERROR, params)
    }
    
    /**
     * Convert TimeRange to analytics value
     */
    private fun TimeRange.toAnalyticsValue(): String {
        return when (this) {
            TimeRange.DAY -> VALUE_TIME_RANGE_WEEK // fallback mapping; consider adding a DAY constant if needed
            TimeRange.WEEK -> VALUE_TIME_RANGE_WEEK
            TimeRange.MONTH -> VALUE_TIME_RANGE_MONTH
            TimeRange.YEAR -> VALUE_TIME_RANGE_YEAR
            TimeRange.ALL_TIME -> VALUE_TIME_RANGE_ALL_TIME
        }
    }
    
    /**
     * Log an analytics event
     */
    private fun logEvent(eventName: String, params: Bundle? = null) {
        // Implementation depends on your analytics SDK (Firebase, Mixpanel, etc.)
        // Example with Firebase:
        // FirebaseAnalytics.getInstance(context).logEvent(eventName, params)
        
        // For now, just log to console
        println("ANALYTICS: $eventName - ${params?.toString() ?: ""}")
    }
    
    /**
     * Log screen view
     */
    fun logScreenView(screenName: String) {
        // Implementation depends on your analytics SDK
        // Example with Firebase:
        // FirebaseAnalytics.getInstance(context).setCurrentScreen(activity, screenName, null)
        
        // For now, just log to console
        println("SCREEN VIEW: $screenName")
    }
}

/**
 * Helper function to log the start of an operation
 */
inline fun <T> logOperation(
    operationName: String,
    block: () -> T
): T {
    val startTime = System.currentTimeMillis()
    println("OPERATION START: $operationName")
    
    return try {
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        println("OPERATION COMPLETE: $operationName took ${duration}ms")
        result
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        println("OPERATION FAILED: $operationName failed after ${duration}ms: ${e.message}")
        throw e
    }
}

/**
 * Helper function to log the start of a coroutine operation
 */
suspend inline fun <T> logSuspendOperation(
    operationName: String,
    crossinline block: suspend () -> T
): T {
    val startTime = System.currentTimeMillis()
    println("SUSPEND OPERATION START: $operationName")
    
    return try {
        val result = block()
        val duration = System.currentTimeMillis() - startTime
        println("SUSPEND OPERATION COMPLETE: $operationName took ${duration}ms")
        result
    } catch (e: Exception) {
        val duration = System.currentTimeMillis() - startTime
        println("SUSPEND OPERATION FAILED: $operationName failed after ${duration}ms: ${e.message}")
        throw e
    }
}
