package com.ainotebuddy.app.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

@Singleton
class PreferencesManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private val THEME_MODE = stringPreferencesKey("theme_mode")
        private val DYNAMIC_COLORS = booleanPreferencesKey("dynamic_colors")
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val AUTO_BACKUP = booleanPreferencesKey("auto_backup")
        private val BIOMETRIC_AUTH = booleanPreferencesKey("biometric_auth")
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { preferences ->
            preferences[THEME_MODE] = mode
        }
    }

    fun getThemeMode(): Flow<String> {
        return context.dataStore.data.map { preferences ->
            preferences[THEME_MODE] ?: "system"
        }
    }

    suspend fun setDynamicColors(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DYNAMIC_COLORS] = enabled
        }
    }

    fun getDynamicColors(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[DYNAMIC_COLORS] ?: true
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = enabled
        }
    }

    fun getNotificationsEnabled(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[NOTIFICATIONS_ENABLED] ?: true
        }
    }

    suspend fun setAutoBackup(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[AUTO_BACKUP] = enabled
        }
    }

    fun getAutoBackup(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[AUTO_BACKUP] ?: false
        }
    }

    suspend fun setBiometricAuth(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[BIOMETRIC_AUTH] = enabled
        }
    }

    fun getBiometricAuth(): Flow<Boolean> {
        return context.dataStore.data.map { preferences ->
            preferences[BIOMETRIC_AUTH] ?: false
        }
    }
}