package com.ainotebuddy.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.util.TimeRange
import com.ainotebuddy.app.data.repository.ExportFormat
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import java.time.LocalDate

private fun timeRangeFromStringOrNull(value: String): TimeRange? = try {
    TimeRange.valueOf(value.uppercase())
} catch (_: IllegalArgumentException) {
    null
}

private val Context.analyticsDataStore: DataStore<Preferences> by preferencesDataStore(name = "analytics_preferences")

/**
 * Manages analytics-related user preferences using DataStore
 */
class AnalyticsPreferences(private val context: Context) {

    private val dataStore = context.analyticsDataStore

    // Preference keys
    private object Keys {
        // Time range
        val TIME_RANGE = stringPreferencesKey("time_range")
        
        // Chart settings
        val SHOW_ANIMATIONS = booleanPreferencesKey("show_animations")
        val CHART_ANIMATION_DURATION = intPreferencesKey("chart_animation_duration")
        val DEFAULT_CHART_TYPE = stringPreferencesKey("default_chart_type")
        
        // Data collection
        val COLLECT_USAGE_STATS = booleanPreferencesKey("collect_usage_stats")
        val SHARE_ANONYMOUS_DATA = booleanPreferencesKey("share_anonymous_data")
        
        // Export settings
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val EXPORT_INCLUDE_CHARTS = booleanPreferencesKey("export_include_charts")
        val EXPORT_INCLUDE_RAW_DATA = booleanPreferencesKey("export_include_raw_data")
        
        // Notification settings
        val NOTIFY_WEEKLY_SUMMARY = booleanPreferencesKey("notify_weekly_summary")
        val NOTIFY_MONTHLY_REPORT = booleanPreferencesKey("notify_monthly_report")
        val NOTIFY_UNUSUAL_ACTIVITY = booleanPreferencesKey("notify_unusual_activity")
        
        // Last sync timestamps
        val LAST_SYNC_TIMESTAMP = longPreferencesKey("last_sync_timestamp")
        val LAST_EXPORT_TIMESTAMP = longPreferencesKey("last_export_timestamp")
        
        // User preferences
        val FAVORITE_METRICS = stringSetPreferencesKey("favorite_metrics")
        val HIDDEN_METRICS = stringSetPreferencesKey("hidden_metrics")
        
        // Advanced settings
        val DATA_RETENTION_DAYS = intPreferencesKey("data_retention_days")
        val AUTO_BACKUP_ENABLED = booleanPreferencesKey("auto_backup_enabled")
        val AUTO_BACKUP_FREQUENCY = stringPreferencesKey("auto_backup_frequency")
    }

    // Default values
    companion object {
        private const val DEFAULT_ANIMATION_DURATION = 1000 // ms
        private const val DEFAULT_DATA_RETENTION_DAYS = 30
        private val DEFAULT_TIME_RANGE = TimeRange.MONTH
        private val DEFAULT_EXPORT_FORMAT = ExportFormat.PDF
    }

    // Time Range Preferences
    val timeRange: Flow<TimeRange> = dataStore.data
        .map { preferences ->
            preferences[Keys.TIME_RANGE]?.let { timeRangeStr ->
                timeRangeFromStringOrNull(timeRangeStr) ?: DEFAULT_TIME_RANGE
            } ?: DEFAULT_TIME_RANGE
        }

    suspend fun setTimeRange(timeRange: TimeRange) {
        dataStore.edit { preferences ->
            preferences[Keys.TIME_RANGE] = timeRange.toString()
        }
    }

    // Chart Settings
    val showAnimations: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.SHOW_ANIMATIONS] ?: true }

    val chartAnimationDuration: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.CHART_ANIMATION_DURATION] ?: DEFAULT_ANIMATION_DURATION }

    // Data Collection
    val collectUsageStats: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.COLLECT_USAGE_STATS] ?: true }

    val shareAnonymousData: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.SHARE_ANONYMOUS_DATA] ?: false }

    // Export Settings
    val defaultExportFormat: Flow<ExportFormat> = dataStore.data
        .map { preferences ->
            preferences[Keys.DEFAULT_EXPORT_FORMAT]?.let { formatStr ->
                try {
                    ExportFormat.valueOf(formatStr.uppercase())
                } catch (e: IllegalArgumentException) {
                    DEFAULT_EXPORT_FORMAT
                }
            } ?: DEFAULT_EXPORT_FORMAT
        }

    val exportIncludeCharts: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.EXPORT_INCLUDE_CHARTS] ?: true }

    val exportIncludeRawData: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.EXPORT_INCLUDE_RAW_DATA] ?: false }

    // Notification Settings
    val notifyWeeklySummary: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.NOTIFY_WEEKLY_SUMMARY] ?: true }

    val notifyMonthlyReport: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.NOTIFY_MONTHLY_REPORT] ?: true }

    val notifyUnusualActivity: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.NOTIFY_UNUSUAL_ACTIVITY] ?: true }

    // Last Sync/Export Timestamps
    val lastSyncTimestamp: Flow<Long> = dataStore.data
        .map { preferences -> preferences[Keys.LAST_SYNC_TIMESTAMP] ?: 0L }

    val lastExportTimestamp: Flow<Long> = dataStore.data
        .map { preferences -> preferences[Keys.LAST_EXPORT_TIMESTAMP] ?: 0L }

    // User Preferences
    val favoriteMetrics: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[Keys.FAVORITE_METRICS] ?: emptySet() }

    val hiddenMetrics: Flow<Set<String>> = dataStore.data
        .map { preferences -> preferences[Keys.HIDDEN_METRICS] ?: emptySet() }

    // Advanced Settings
    val dataRetentionDays: Flow<Int> = dataStore.data
        .map { preferences -> preferences[Keys.DATA_RETENTION_DAYS] ?: DEFAULT_DATA_RETENTION_DAYS }

    val autoBackupEnabled: Flow<Boolean> = dataStore.data
        .map { preferences -> preferences[Keys.AUTO_BACKUP_ENABLED] ?: false }

    val autoBackupFrequency: Flow<String> = dataStore.data
        .map { preferences -> preferences[Keys.AUTO_BACKUP_FREQUENCY] ?: "weekly" }

    // Helper functions
    suspend fun updateLastSyncTimestamp() {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_SYNC_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun updateLastExportTimestamp() {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_EXPORT_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun addFavoriteMetric(metricId: String) {
        dataStore.edit { preferences ->
            val currentFavorites = preferences[Keys.FAVORITE_METRICS]?.toMutableSet() ?: mutableSetOf()
            currentFavorites.add(metricId)
            preferences[Keys.FAVORITE_METRICS] = currentFavorites
            
            // Remove from hidden metrics if present
            val hiddenMetrics = preferences[Keys.HIDDEN_METRICS]?.toMutableSet() ?: mutableSetOf()
            hiddenMetrics.remove(metricId)
            preferences[Keys.HIDDEN_METRICS] = hiddenMetrics
        }
    }

    suspend fun removeFavoriteMetric(metricId: String) {
        dataStore.edit { preferences ->
            val currentFavorites = preferences[Keys.FAVORITE_METRICS]?.toMutableSet() ?: return@edit
            currentFavorites.remove(metricId)
            preferences[Keys.FAVORITE_METRICS] = currentFavorites
        }
    }

    suspend fun hideMetric(metricId: String) {
        dataStore.edit { preferences ->
            val hiddenMetrics = preferences[Keys.HIDDEN_METRICS]?.toMutableSet() ?: mutableSetOf()
            hiddenMetrics.add(metricId)
            preferences[Keys.HIDDEN_METRICS] = hiddenMetrics
            
            // Remove from favorites if present
            val favorites = preferences[Keys.FAVORITE_METRICS]?.toMutableSet() ?: mutableSetOf()
            favorites.remove(metricId)
            preferences[Keys.FAVORITE_METRICS] = favorites
        }
    }

    suspend fun showMetric(metricId: String) {
        dataStore.edit { preferences ->
            val hiddenMetrics = preferences[Keys.HIDDEN_METRICS]?.toMutableSet() ?: return@edit
            hiddenMetrics.remove(metricId)
            preferences[Keys.HIDDEN_METRICS] = hiddenMetrics
        }
    }

    // Reset all preferences to default
    suspend fun resetToDefaults() {
        dataStore.edit { preferences ->
            preferences.clear()
            // Set default values
            preferences[Keys.TIME_RANGE] = DEFAULT_TIME_RANGE.toString()
            preferences[Keys.SHOW_ANIMATIONS] = true
            preferences[Keys.CHART_ANIMATION_DURATION] = DEFAULT_ANIMATION_DURATION
            preferences[Keys.COLLECT_USAGE_STATS] = true
            preferences[Keys.SHARE_ANONYMOUS_DATA] = false
            preferences[Keys.DEFAULT_EXPORT_FORMAT] = DEFAULT_EXPORT_FORMAT.name
            preferences[Keys.EXPORT_INCLUDE_CHARTS] = true
            preferences[Keys.EXPORT_INCLUDE_RAW_DATA] = false
            preferences[Keys.NOTIFY_WEEKLY_SUMMARY] = true
            preferences[Keys.NOTIFY_MONTHLY_REPORT] = true
            preferences[Keys.NOTIFY_UNUSUAL_ACTIVITY] = true
            preferences[Keys.DATA_RETENTION_DAYS] = DEFAULT_DATA_RETENTION_DAYS
            preferences[Keys.AUTO_BACKUP_ENABLED] = false
            preferences[Keys.AUTO_BACKUP_FREQUENCY] = "weekly"
        }
    }

    // Check if data is stale (older than retention period)
    suspend fun isDataStale(): Boolean {
        val retentionDays = dataRetentionDays.first()
        val lastSync = lastSyncTimestamp.first()
        
        if (lastSync == 0L) return true // No data yet
        
        val retentionMillis = retentionDays * 24 * 60 * 60 * 1000L
        return (System.currentTimeMillis() - lastSync) > retentionMillis
    }

    // Check if it's time for a backup
    suspend fun isBackupDue(): Boolean {
        if (!autoBackupEnabled.first()) return false
        
        val lastBackup = lastExportTimestamp.first()
        if (lastBackup == 0L) return true // Never backed up
        
        val frequency = autoBackupFrequency.first()
        val frequencyMillis = when (frequency) {
            "daily" -> 24 * 60 * 60 * 1000L
            "weekly" -> 7 * 24 * 60 * 60 * 1000L
            "monthly" -> 30L * 24 * 60 * 60 * 1000L
            else -> 7 * 24 * 60 * 60 * 1000L // Default to weekly
        }
        
        return (System.currentTimeMillis() - lastBackup) >= frequencyMillis
    }
}
