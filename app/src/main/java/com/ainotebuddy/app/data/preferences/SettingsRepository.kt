package com.ainotebuddy.app.data.preferences

  import android.content.Context
  import androidx.datastore.core.DataStore
  import androidx.datastore.preferences.core.*
  import androidx.datastore.preferences.preferencesDataStore
  import dagger.hilt.android.qualifiers.ApplicationContext
  import kotlinx.coroutines.flow.Flow
  import kotlinx.coroutines.flow.first
  import kotlinx.coroutines.flow.map
  import javax.inject.Inject
  import javax.inject.Singleton

/**
 * Repository for managing user preferences and settings
 */

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

  @Singleton
  class SettingsRepository @Inject constructor(
      @ApplicationContext private val context: Context
  ) {
    
    // Preference Keys
    object PreferenceKeys {
        // AI Settings
        val AI_ANALYSIS_ENABLED = booleanPreferencesKey("ai_analysis_enabled")
        val AUTO_SUGGESTIONS_ENABLED = booleanPreferencesKey("auto_suggestions_enabled")
        val SENTIMENT_ANALYSIS_ENABLED = booleanPreferencesKey("sentiment_analysis_enabled")
        val AI_PROCESSING_LEVEL = stringPreferencesKey("ai_processing_level")
        val SUGGESTION_CONFIDENCE_THRESHOLD = floatPreferencesKey("suggestion_confidence_threshold")
        val AUTO_APPLY_SUGGESTIONS = booleanPreferencesKey("auto_apply_suggestions")
        
        // Theme Settings
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DARK_MODE_ENABLED = booleanPreferencesKey("dark_mode_enabled")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val ACCENT_COLOR = stringPreferencesKey("accent_color")
        
        // Text and Display Settings
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val TEXT_SCALING = floatPreferencesKey("text_scaling")
        val SHOW_LINE_NUMBERS = booleanPreferencesKey("show_line_numbers")
        
        // Voice Settings
        val VOICE_ENABLED = booleanPreferencesKey("voice_enabled")
        val VOICE_LANGUAGE = stringPreferencesKey("voice_language")
        val TTS_ENABLED = booleanPreferencesKey("tts_enabled")
        val TTS_SPEED = floatPreferencesKey("tts_speed")
        val TTS_PITCH = floatPreferencesKey("tts_pitch")
        
        // Sync and Backup Settings
        val AUTO_SYNC_ENABLED = booleanPreferencesKey("auto_sync_enabled")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
        val CLOUD_SYNC_ENABLED = booleanPreferencesKey("cloud_sync_enabled")
        val LAST_BACKUP_TIME = longPreferencesKey("last_backup_time")
        
        // Privacy and Security Settings
        val BIOMETRIC_AUTH_ENABLED = booleanPreferencesKey("biometric_auth_enabled")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val DATA_ENCRYPTION_ENABLED = booleanPreferencesKey("data_encryption_enabled")
        val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
        
        // Notification Settings
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val REMINDER_NOTIFICATIONS = booleanPreferencesKey("reminder_notifications")
        val SYNC_NOTIFICATIONS = booleanPreferencesKey("sync_notifications")
        val AI_INSIGHTS_NOTIFICATIONS = booleanPreferencesKey("ai_insights_notifications")
        
        // Reminder Settings
        val REMINDER_DEFAULT_HOUR = intPreferencesKey("reminder_default_hour")
        val REMINDER_DEFAULT_MINUTE = intPreferencesKey("reminder_default_minute")
        
        // Editor Settings
        val AUTO_SAVE_ENABLED = booleanPreferencesKey("auto_save_enabled")
        val AUTO_SAVE_INTERVAL = intPreferencesKey("auto_save_interval")
        val MARKDOWN_PREVIEW_ENABLED = booleanPreferencesKey("markdown_preview_enabled")
        val SPELL_CHECK_ENABLED = booleanPreferencesKey("spell_check_enabled")
        val WORD_WRAP_ENABLED = booleanPreferencesKey("word_wrap_enabled")
        
        // Search Settings
        val SEARCH_HISTORY_ENABLED = booleanPreferencesKey("search_history_enabled")
        val FUZZY_SEARCH_ENABLED = booleanPreferencesKey("fuzzy_search_enabled")
        val SEARCH_IN_CONTENT = booleanPreferencesKey("search_in_content")
        val CASE_SENSITIVE_SEARCH = booleanPreferencesKey("case_sensitive_search")
        
        // Export Settings
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val INCLUDE_METADATA_IN_EXPORT = booleanPreferencesKey("include_metadata_in_export")
        val EXPORT_LOCATION = stringPreferencesKey("export_location")
        
        // Performance Settings
        val HARDWARE_ACCELERATION = booleanPreferencesKey("hardware_acceleration")
        val MEMORY_OPTIMIZATION = booleanPreferencesKey("memory_optimization")
        val LAZY_LOADING_ENABLED = booleanPreferencesKey("lazy_loading_enabled")

        // AI Background Processing
        val PAUSE_AI_PROCESSING = booleanPreferencesKey("pause_ai_processing")
        
        // Accessibility Settings
        val HIGH_CONTRAST_MODE = booleanPreferencesKey("high_contrast_mode")
        val LARGE_TEXT_MODE = booleanPreferencesKey("large_text_mode")
        val SCREEN_READER_SUPPORT = booleanPreferencesKey("screen_reader_support")
        val REDUCE_ANIMATIONS = booleanPreferencesKey("reduce_animations")
        
        // App Behavior Settings
        val CONFIRM_DELETE_ENABLED = booleanPreferencesKey("confirm_delete_enabled")
        val SHOW_TUTORIAL = booleanPreferencesKey("show_tutorial")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
        val APP_VERSION = stringPreferencesKey("app_version")
        val LAST_USED_TIMESTAMP = longPreferencesKey("last_used_timestamp")
    }
    
    // AI Settings Flows
    val aiAnalysisEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AI_ANALYSIS_ENABLED] ?: true
    }
    
    val autoSuggestionsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_SUGGESTIONS_ENABLED] ?: true
    }
    
    val sentimentAnalysisEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SENTIMENT_ANALYSIS_ENABLED] ?: true
    }
    
    val aiProcessingLevel: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AI_PROCESSING_LEVEL] ?: "BALANCED"
    }
    
    val suggestionConfidenceThreshold: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SUGGESTION_CONFIDENCE_THRESHOLD] ?: 0.7f
    }
    
    val autoApplySuggestions: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_APPLY_SUGGESTIONS] ?: false
    }
    
    // Theme Settings Flows
    val themeMode: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.THEME_MODE] ?: "SYSTEM"
    }
    
    val darkModeEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DARK_MODE_ENABLED] ?: false
    }
    
    val dynamicColorEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DYNAMIC_COLOR_ENABLED] ?: true
    }
    
    val fontSize: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.FONT_SIZE] ?: 14f
    }
    
    val textScaling: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.TEXT_SCALING] ?: 1.0f
    }
    
    // Voice Settings Flows
    val voiceEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.VOICE_ENABLED] ?: true
    }
    
    val voiceLanguage: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.VOICE_LANGUAGE] ?: "en-US"
    }
    
    val ttsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.TTS_ENABLED] ?: true
    }
    
    val ttsSpeed: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.TTS_SPEED] ?: 1.0f
    }
    
    val ttsPitch: Flow<Float> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.TTS_PITCH] ?: 1.0f
    }
    
    // Sync Settings Flows
    val autoSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_SYNC_ENABLED] ?: false
    }
    
    val cloudSyncEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.CLOUD_SYNC_ENABLED] ?: false
    }
    
    val backupFrequency: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BACKUP_FREQUENCY] ?: "WEEKLY"
    }
    
    // Security Settings Flows
    val biometricAuthEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.BIOMETRIC_AUTH_ENABLED] ?: false
    }
    
    val appLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.APP_LOCK_ENABLED] ?: false
    }
    
    val dataEncryptionEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.DATA_ENCRYPTION_ENABLED] ?: true
    }
    
    // Editor Settings Flows
    val autoSaveEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_SAVE_ENABLED] ?: true
    }
    
    val autoSaveInterval: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.AUTO_SAVE_INTERVAL] ?: 30 // seconds
    }
    
    val markdownPreviewEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.MARKDOWN_PREVIEW_ENABLED] ?: true
    }
    
    // Notification Settings Flows
    val notificationsEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] ?: true
    }
    
    val reminderNotifications: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.REMINDER_NOTIFICATIONS] ?: true
    }
    
    // Reminder default time (hour/minute)
    val reminderDefaultHour: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.REMINDER_DEFAULT_HOUR] ?: 9
    }
    val reminderDefaultMinute: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.REMINDER_DEFAULT_MINUTE] ?: 0
    }
    
    // AI Background Processing Flow
    val pauseAIProcessing: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.PAUSE_AI_PROCESSING] ?: false
    }

    // App Behavior Flows
    val confirmDeleteEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.CONFIRM_DELETE_ENABLED] ?: true
    }
    
    val showTutorial: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.SHOW_TUTORIAL] ?: true
    }
    
    val firstLaunch: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferenceKeys.FIRST_LAUNCH] ?: true
    }
    
    // Settings Update Functions
    suspend fun setAIAnalysisEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AI_ANALYSIS_ENABLED] = enabled
        }
    }
    
    suspend fun setAutoSuggestionsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_SUGGESTIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setSentimentAnalysisEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SENTIMENT_ANALYSIS_ENABLED] = enabled
        }
    }
    
    suspend fun setAIProcessingLevel(level: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AI_PROCESSING_LEVEL] = level
        }
    }
    
    suspend fun setSuggestionConfidenceThreshold(threshold: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SUGGESTION_CONFIDENCE_THRESHOLD] = threshold
        }
    }
    
    suspend fun setAutoApplySuggestions(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_APPLY_SUGGESTIONS] = enabled
        }
    }
    
    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.THEME_MODE] = mode
        }
    }
    
    suspend fun setDarkModeEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.DARK_MODE_ENABLED] = enabled
        }
    }
    
    suspend fun setFontSize(size: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.FONT_SIZE] = size
        }
    }
    
    suspend fun setTextScaling(scaling: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.TEXT_SCALING] = scaling
        }
    }
    
    suspend fun setVoiceEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.VOICE_ENABLED] = enabled
        }
    }
    
    suspend fun setVoiceLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.VOICE_LANGUAGE] = language
        }
    }
    
    suspend fun setTTSEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.TTS_ENABLED] = enabled
        }
    }
    
    suspend fun setTTSSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.TTS_SPEED] = speed
        }
    }
    
    suspend fun setTTSPitch(pitch: Float) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.TTS_PITCH] = pitch
        }
    }
    
    suspend fun setAutoSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_SYNC_ENABLED] = enabled
        }
    }
    
    suspend fun setCloudSyncEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.CLOUD_SYNC_ENABLED] = enabled
        }
    }
    
    suspend fun setBiometricAuthEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.BIOMETRIC_AUTH_ENABLED] = enabled
        }
    }
    
    suspend fun setAppLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.APP_LOCK_ENABLED] = enabled
        }
    }
    
    suspend fun setAutoSaveEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_SAVE_ENABLED] = enabled
        }
    }

    suspend fun setPauseAIProcessing(paused: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.PAUSE_AI_PROCESSING] = paused
        }
    }
    
    suspend fun setAutoSaveInterval(interval: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.AUTO_SAVE_INTERVAL] = interval
        }
    }
    
    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.NOTIFICATIONS_ENABLED] = enabled
        }
    }
    
    suspend fun setConfirmDeleteEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.CONFIRM_DELETE_ENABLED] = enabled
        }
    }

    // Reminder defaults setters
    suspend fun setReminderDefaultTime(hour: Int, minute: Int) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.REMINDER_DEFAULT_HOUR] = hour.coerceIn(0, 23)
            preferences[PreferenceKeys.REMINDER_DEFAULT_MINUTE] = minute.coerceIn(0, 59)
        }
    }
    
    suspend fun setShowTutorial(show: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.SHOW_TUTORIAL] = show
        }
    }
    
    suspend fun setFirstLaunch(isFirst: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.FIRST_LAUNCH] = isFirst
        }
    }
    
    suspend fun updateLastUsedTimestamp() {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.LAST_USED_TIMESTAMP] = System.currentTimeMillis()
        }
    }
    
    suspend fun setAppVersion(version: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferenceKeys.APP_VERSION] = version
        }
    }
    
    // Bulk Operations
    suspend fun resetAISettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.AI_ANALYSIS_ENABLED)
            preferences.remove(PreferenceKeys.AUTO_SUGGESTIONS_ENABLED)
            preferences.remove(PreferenceKeys.SENTIMENT_ANALYSIS_ENABLED)
            preferences.remove(PreferenceKeys.AI_PROCESSING_LEVEL)
            preferences.remove(PreferenceKeys.SUGGESTION_CONFIDENCE_THRESHOLD)
            preferences.remove(PreferenceKeys.AUTO_APPLY_SUGGESTIONS)
        }
    }
    
    suspend fun resetThemeSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.THEME_MODE)
            preferences.remove(PreferenceKeys.DARK_MODE_ENABLED)
            preferences.remove(PreferenceKeys.DYNAMIC_COLOR_ENABLED)
            preferences.remove(PreferenceKeys.FONT_SIZE)
            preferences.remove(PreferenceKeys.TEXT_SCALING)
        }
    }
    
    suspend fun resetVoiceSettings() {
        context.dataStore.edit { preferences ->
            preferences.remove(PreferenceKeys.VOICE_ENABLED)
            preferences.remove(PreferenceKeys.VOICE_LANGUAGE)
            preferences.remove(PreferenceKeys.TTS_ENABLED)
            preferences.remove(PreferenceKeys.TTS_SPEED)
            preferences.remove(PreferenceKeys.TTS_PITCH)
        }
    }
    
    suspend fun resetAllSettings() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
    
    suspend fun exportSettings(): Map<String, Any?> {
        val preferences = context.dataStore.data.first()
        return preferences.asMap().mapKeys { it.key.name }
    }
    
    suspend fun importSettings(settings: Map<String, Any?>) {
        context.dataStore.edit { preferences ->
            settings.forEach { (key, value) ->
                when (value) {
                    is Boolean -> preferences[booleanPreferencesKey(key)] = value
                    is String -> preferences[stringPreferencesKey(key)] = value
                    is Int -> preferences[intPreferencesKey(key)] = value
                    is Long -> preferences[longPreferencesKey(key)] = value
                    is Float -> preferences[floatPreferencesKey(key)] = value
                    is Double -> preferences[doublePreferencesKey(key)] = value
                }
            }
        }
    }
}