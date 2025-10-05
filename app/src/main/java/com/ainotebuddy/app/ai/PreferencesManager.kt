package com.ainotebuddy.app.ai

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "ai_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val OPENAI_KEY = stringPreferencesKey("openai_key")
        private val GEMINI_KEY = stringPreferencesKey("gemini_key")
        private val CLAUDE_KEY = stringPreferencesKey("claude_key")
        private val AI_PROVIDER = stringPreferencesKey("ai_provider")
    }

    suspend fun setOpenAIKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[OPENAI_KEY] = key
        }
    }

    fun getOpenAIKey(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[OPENAI_KEY] ?: ""
        }
    }

    suspend fun setGeminiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[GEMINI_KEY] = key
        }
    }

    fun getGeminiKey(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[GEMINI_KEY] ?: ""
        }
    }

    suspend fun setClaudeKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[CLAUDE_KEY] = key
        }
    }

    fun getClaudeKey(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[CLAUDE_KEY] ?: ""
        }
    }

    suspend fun setAIProvider(provider: AIProvider) {
        context.dataStore.edit { preferences ->
            preferences[AI_PROVIDER] = provider.name
        }
    }

    fun getAIProvider(): Flow<AIProvider> {
        return context.dataStore.data.map { preferences ->
            val providerName = preferences[AI_PROVIDER] ?: AIProvider.OPENAI.name
            AIProvider.valueOf(providerName)
        }
    }
}