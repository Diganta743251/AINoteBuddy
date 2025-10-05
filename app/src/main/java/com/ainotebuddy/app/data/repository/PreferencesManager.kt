package com.ainotebuddy.app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for application preferences
 */
@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    
    // AI-related preferences
    suspend fun getAIEnabled(): Boolean = sharedPrefs.getBoolean("ai_enabled", true)
    suspend fun setAIEnabled(enabled: Boolean) = sharedPrefs.edit().putBoolean("ai_enabled", enabled).apply()
    
    suspend fun getAutoSuggestionsEnabled(): Boolean = sharedPrefs.getBoolean("auto_suggestions", true)
    suspend fun setAutoSuggestionsEnabled(enabled: Boolean) = sharedPrefs.edit().putBoolean("auto_suggestions", enabled).apply()
    
    suspend fun getSentimentAnalysisEnabled(): Boolean = sharedPrefs.getBoolean("sentiment_analysis", false)
    suspend fun setSentimentAnalysisEnabled(enabled: Boolean) = sharedPrefs.edit().putBoolean("sentiment_analysis", enabled).apply()
    
    // Voice command preferences
    suspend fun getVoiceCommandsEnabled(): Boolean = sharedPrefs.getBoolean("voice_commands", false)
    suspend fun setVoiceCommandsEnabled(enabled: Boolean) = sharedPrefs.edit().putBoolean("voice_commands", enabled).apply()
    
    // General app preferences
    suspend fun getTheme(): String = sharedPrefs.getString("theme", "SYSTEM") ?: "SYSTEM"
    suspend fun setTheme(theme: String) = sharedPrefs.edit().putString("theme", theme).apply()
    
    // API Keys
    fun getOpenAIKey(): String? = sharedPrefs.getString("openai_key", null)
    fun setOpenAIKey(key: String?) = sharedPrefs.edit().putString("openai_key", key).apply()
    
    fun getGeminiKey(): String? = sharedPrefs.getString("gemini_key", null)
    fun setGeminiKey(key: String?) = sharedPrefs.edit().putString("gemini_key", key).apply()
}