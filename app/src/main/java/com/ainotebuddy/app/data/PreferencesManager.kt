package com.ainotebuddy.app.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.ainotebuddy.app.data.model.UIState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure preferences manager for API keys and app settings
 */
@Singleton
class PreferencesManager @Inject constructor(@ApplicationContext private val context: Context) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()
    
    private val encryptedPrefs = EncryptedSharedPreferences.create(
        context,
        "ai_notebuddy_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    private val regularPrefs = context.getSharedPreferences("ai_notebuddy_prefs", Context.MODE_PRIVATE)
    
    // AI Provider Settings
    private val _aiProvider = MutableStateFlow(getAIProvider())
    val aiProvider: Flow<AIProvider> = _aiProvider.asStateFlow()
    
    private val _hasValidApiKey = MutableStateFlow(hasValidApiKey())
    val hasValidApiKey: Flow<Boolean> = _hasValidApiKey.asStateFlow()
    
    // UI State
    private val _uiState = MutableStateFlow(loadUIState())
    val uiState: Flow<UIState> = _uiState.asStateFlow()
    
    // High Contrast Mode
    private val _highContrastMode = MutableStateFlow(getHighContrastMode())
    val highContrastMode: Flow<Boolean> = _highContrastMode.asStateFlow()
    
    private val json = Json { ignoreUnknownKeys = true }
    
    // API Keys (Encrypted)
    fun setOpenAIKey(key: String) {
        encryptedPrefs.edit().putString(OPENAI_API_KEY, key).apply()
        _hasValidApiKey.value = hasValidApiKey()
    }
    
    fun getOpenAIKey(): String? = encryptedPrefs.getString(OPENAI_API_KEY, null)
    
    fun setGeminiKey(key: String) {
        encryptedPrefs.edit().putString(GEMINI_API_KEY, key).apply()
        _hasValidApiKey.value = hasValidApiKey()
    }
    
    fun getGeminiKey(): String? = encryptedPrefs.getString(GEMINI_API_KEY, null)
    
    fun setClaudeKey(key: String) {
        encryptedPrefs.edit().putString(CLAUDE_API_KEY, key).apply()
        _hasValidApiKey.value = hasValidApiKey()
    }
    
    fun getClaudeKey(): String? = encryptedPrefs.getString(CLAUDE_API_KEY, null)
    
    // AI Provider Selection
    fun setAIProvider(provider: AIProvider) {
        regularPrefs.edit().putString(AI_PROVIDER, provider.name).apply()
        _aiProvider.value = provider
        _hasValidApiKey.value = hasValidApiKey()
    }
    
    fun getAIProvider(): AIProvider {
        val providerName = regularPrefs.getString(AI_PROVIDER, AIProvider.OPENAI.name)
        return AIProvider.values().find { it.name == providerName } ?: AIProvider.OPENAI
    }
    
    // AI Settings
    fun setAITemperature(temperature: Float) {
        regularPrefs.edit().putFloat(AI_TEMPERATURE, temperature).apply()
    }
    
    fun getAITemperature(): Float = regularPrefs.getFloat(AI_TEMPERATURE, 0.7f)
    
    fun setMaxTokens(tokens: Int) {
        regularPrefs.edit().putInt(MAX_TOKENS, tokens).apply()
    }

    fun getMaxTokens(): Int = regularPrefs.getInt(MAX_TOKENS, 1000)

    fun setAIModel(model: String) {
        regularPrefs.edit().putString(AI_MODEL, model).apply()
    }
    
    fun getAIModel(): String {
        return when (getAIProvider()) {
            AIProvider.OPENAI -> regularPrefs.getString(AI_MODEL, "gpt-3.5-turbo") ?: "gpt-3.5-turbo"
            AIProvider.GEMINI -> regularPrefs.getString(AI_MODEL, "gemini-pro") ?: "gemini-pro"
            AIProvider.CLAUDE -> regularPrefs.getString(AI_MODEL, "claude-3-sonnet-20240229") ?: "claude-3-sonnet-20240229"
            AIProvider.OFFLINE -> "offline"
        }
    }
    
    // Feature Toggles
    fun setAutoEnhanceEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(AUTO_ENHANCE_ENABLED, enabled).apply()
    }

    fun isAutoEnhanceEnabled(): Boolean = regularPrefs.getBoolean(AUTO_ENHANCE_ENABLED, false)

    fun setVoiceProcessingEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(VOICE_PROCESSING_ENABLED, enabled).apply()
    }

    fun isVoiceProcessingEnabled(): Boolean = regularPrefs.getBoolean(VOICE_PROCESSING_ENABLED, true)

    fun setSmartCategorizationEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(SMART_CATEGORIZATION_ENABLED, enabled).apply()
    }
    
    fun isSmartCategorizationEnabled(): Boolean = regularPrefs.getBoolean(SMART_CATEGORIZATION_ENABLED, true)

    // Analytics & Backup preferences
    fun setAnalyticsEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(ANALYTICS_ENABLED, enabled).apply()
    }

    fun getAnalyticsEnabled(): Boolean {
        return regularPrefs.getBoolean(ANALYTICS_ENABLED, true)
    }

    fun setBackupEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(BACKUP_ENABLED, enabled).apply()
    }

    fun getBackupEnabled(): Boolean {
        return regularPrefs.getBoolean(BACKUP_ENABLED, true)
    }
    
    // Generic API Key methods for ViewModel
    fun setApiKey(provider: AIProvider, key: String) {
        when (provider) {
            AIProvider.OPENAI -> setOpenAIKey(key)
            AIProvider.GEMINI -> setGeminiKey(key)
            AIProvider.CLAUDE -> setClaudeKey(key)
            AIProvider.OFFLINE -> {} // No API key needed
        }
    }
    
    fun getApiKey(provider: AIProvider): String {
        return when (provider) {
            AIProvider.OPENAI -> getOpenAIKey() ?: ""
            AIProvider.GEMINI -> getGeminiKey() ?: ""
            AIProvider.CLAUDE -> getClaudeKey() ?: ""
            AIProvider.OFFLINE -> ""
        }
    }
    
    fun hasValidApiKey(): Boolean {
        return when (getAIProvider()) {
            AIProvider.OPENAI -> !getOpenAIKey().isNullOrBlank()
            AIProvider.GEMINI -> !getGeminiKey().isNullOrBlank()
            AIProvider.CLAUDE -> !getClaudeKey().isNullOrBlank()
            AIProvider.OFFLINE -> true
        }
    }
    
    // Alias methods for ViewModel compatibility
    fun getSelectedModel(): String = getAIModel()
    fun setSelectedModel(model: String) = setAIModel(model)
    fun getTemperature(): Float = getAITemperature()
    fun setTemperature(temperature: Float) = setAITemperature(temperature)
    
    // Usage Statistics
    fun incrementAIUsage() {
        val count = regularPrefs.getInt(AI_USAGE_COUNT, 0) + 1
        regularPrefs.edit()
            .putInt(AI_USAGE_COUNT, count)
            .putLong(LAST_AI_USAGE, System.currentTimeMillis())
            .apply()
    }
    
    fun getAIUsageCount(): Int = regularPrefs.getInt(AI_USAGE_COUNT, 0)
    
    fun getLastAIUsage(): Long = regularPrefs.getLong(LAST_AI_USAGE, 0)
    
    // Clear all API keys (for logout)
    fun clearAllApiKeys() {
        encryptedPrefs.edit()
            .remove(OPENAI_API_KEY)
            .remove(GEMINI_API_KEY)
            .remove(CLAUDE_API_KEY)
            .apply()
        
        // Reset provider to default (OPENAI)
        setAIProvider(AIProvider.OPENAI)
        _hasValidApiKey.value = false
    }
    
    // Validate API key format (basic validation)
    fun validateApiKey(provider: AIProvider, key: String): Boolean {
        if (key.isBlank()) return false
        
        return when (provider) {
            AIProvider.OPENAI -> key.startsWith("sk-") && key.length > 30
            AIProvider.GEMINI -> key.length >= 20 // Gemini keys are usually longer
            AIProvider.CLAUDE -> key.startsWith("sk-ant-") && key.length > 40
            AIProvider.OFFLINE -> true
        }
    }
    
    // Theme and appearance
    fun setTheme(theme: String) {
        regularPrefs.edit().putString(THEME_PREF, theme).apply()
    }
    
    fun getTheme(): String {
        return regularPrefs.getString(THEME_PREF, THEME_SYSTEM) ?: THEME_SYSTEM
    }
    
    fun setDynamicColors(enabled: Boolean) {
        regularPrefs.edit().putBoolean(DYNAMIC_COLORS_PREF, enabled).apply()
    }
    
    fun getUseDynamicColors(): Boolean {
        return regularPrefs.getBoolean(DYNAMIC_COLORS_PREF, true)
    }
    
    fun setFontScale(scale: Float) {
        regularPrefs.edit().putFloat(FONT_SCALE, scale).apply()
    }
    
    fun getFontScale(): Float {
        return regularPrefs.getFloat(FONT_SCALE, 1f)
    }
    
    fun setHighContrastMode(enabled: Boolean) {
        regularPrefs.edit().putBoolean(HIGH_CONTRAST_MODE, enabled).apply()
        _highContrastMode.value = enabled
    }
    
    fun getHighContrastMode(): Boolean {
        return regularPrefs.getBoolean(HIGH_CONTRAST_MODE, false)
    }
    
    // Text-to-Speech settings
    fun setTtsEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(TTS_ENABLED, enabled).apply()
    }
    
    fun getTtsEnabled(): Boolean {
        return regularPrefs.getBoolean(TTS_ENABLED, false)
    }
    
    fun setTtsRate(rate: Float) {
        regularPrefs.edit().putFloat(TTS_RATE, rate.coerceIn(0.5f, 2.0f)).apply()
    }
    
    fun getTtsRate(): Float {
        return regularPrefs.getFloat(TTS_RATE, 1.0f)
    }
    
    // Keyboard navigation settings
    fun setKeyboardNavEnabled(enabled: Boolean) {
        regularPrefs.edit().putBoolean(KEYBOARD_NAV_ENABLED, enabled).apply()
    }
    
    fun getKeyboardNavEnabled(): Boolean {
        return regularPrefs.getBoolean(KEYBOARD_NAV_ENABLED, true)
    }
    
    // Onboarding completion status
    fun setOnboardingCompleted(completed: Boolean) {
        regularPrefs.edit().putBoolean(ONBOARDING_COMPLETED, completed).apply()
    }
    
    fun isOnboardingCompleted(): Boolean = regularPrefs.getBoolean(ONBOARDING_COMPLETED, false)
    
    // UI State Management
    fun saveUIState(uiState: UIState) {
        val jsonString = json.encodeToString(uiState)
        regularPrefs.edit().putString(UI_STATE, jsonString).apply()
        _uiState.value = uiState
    }
    
    fun loadUIState(): UIState {
        val jsonString = regularPrefs.getString(UI_STATE, null) ?: return UIState()
        return try {
            json.decodeFromString(jsonString) ?: UIState()
        } catch (e: Exception) {
            // If there's an error parsing, return default state
            UIState()
        }
    }
    
    // Individual state updates
    fun saveExpandedStates(expandedStates: Map<String, Boolean>) {
        val currentState = _uiState.value
        saveUIState(currentState.copy(expandedStates = expandedStates))
    }
    
    fun saveSelectedNoteIds(selectedNoteIds: Set<String>) {
        val currentState = _uiState.value
        saveUIState(currentState.copy(selectedNoteIds = selectedNoteIds))
    }
    
    fun saveViewMode(viewMode: String) {
        val currentState = _uiState.value
        saveUIState(currentState.copy(viewMode = viewMode))
    }
    
    fun clearUIState() {
        saveUIState(UIState())
    }
    
    fun getViewMode(): String {
        return loadUIState().viewMode
    }
    
    companion object {
        // Existing constants
        const val OPENAI_API_KEY = "openai_api_key"
        const val GEMINI_API_KEY = "gemini_api_key"
        const val CLAUDE_API_KEY = "claude_api_key"
        const val AI_PROVIDER = "ai_provider"
        const val AI_TEMPERATURE = "ai_temperature"
        const val MAX_TOKENS = "max_tokens"
        const val AI_MODEL = "ai_model"
        const val AUTO_ENHANCE_ENABLED = "auto_enhance_enabled"
        const val VOICE_PROCESSING_ENABLED = "voice_processing_enabled"
        const val SMART_CATEGORIZATION_ENABLED = "smart_categorization_enabled"
        const val AI_USAGE_COUNT = "ai_usage_count"
        const val LAST_AI_USAGE = "last_ai_usage"
        
        // Theme and appearance
        const val THEME_SYSTEM = "system"
        const val THEME_LIGHT = "light"
        const val THEME_DARK = "dark"
        const val THEME_PREF = "app_theme"
        const val DYNAMIC_COLORS_PREF = "dynamic_colors"
        const val FONT_SCALE = "font_scale"
        const val HIGH_CONTRAST_MODE = "high_contrast_mode"
        
        // Text-to-Speech
        const val TTS_ENABLED = "tts_enabled"
        const val TTS_RATE = "tts_rate"
        
        // Keyboard navigation
        const val KEYBOARD_NAV_ENABLED = "keyboard_nav_enabled"
        
        // UI State
        const val UI_STATE = "ui_state"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        
        // Analytics & Backup keys
        const val ANALYTICS_ENABLED = "analytics_enabled"
        const val BACKUP_ENABLED = "backup_enabled"

    }
}
