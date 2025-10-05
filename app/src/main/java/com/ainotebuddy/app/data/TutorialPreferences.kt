package com.ainotebuddy.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tutorialDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "tutorial_preferences"
)

/**
 * Manages tutorial progress and preferences using DataStore
 */
class TutorialPreferences(private val context: Context) {
    private val dataStore = context.tutorialDataStore

    // Keys for tutorial progress
    private object Keys {
        val COMPLETED_TUTORIALS = stringSetPreferencesKey("completed_tutorials")
        val LAST_SHOWN_VERSION = intPreferencesKey("tutorial_version")
        val SHOW_TUTORIAL = booleanPreferencesKey("show_tutorial")
    }

    // Current version of the tutorials (increment when updating tutorial content)
    private val CURRENT_TUTORIAL_VERSION = 1

    /**
     * Flow of completed tutorial IDs
     */
    val completedTutorials: Flow<Set<String>> = dataStore.data
        .map { preferences ->
            preferences[Keys.COMPLETED_TUTORIALS] ?: emptySet()
        }

    /**
     * Check if a tutorial should be shown based on completion status and version
     */
    val shouldShowTutorial: Flow<Boolean> = dataStore.data
        .map { preferences ->
            val lastShownVersion = preferences[Keys.LAST_SHOWN_VERSION] ?: 0
            val showByDefault = preferences[Keys.SHOW_TUTORIAL] ?: true
            
            showByDefault && lastShownVersion < CURRENT_TUTORIAL_VERSION
        }

    /**
     * Mark a tutorial as completed
     */
    suspend fun markTutorialCompleted(tutorialId: String) {
        dataStore.edit { preferences ->
            val completed = preferences[Keys.COMPLETED_TUTORIALS]?.toMutableSet() ?: mutableSetOf()
            completed.add(tutorialId)
            preferences[Keys.COMPLETED_TUTORIALS] = completed
            preferences[Keys.LAST_SHOWN_VERSION] = CURRENT_TUTORIAL_VERSION
        }
    }

    /**
     * Reset tutorial progress
     */
    suspend fun resetTutorialProgress() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.COMPLETED_TUTORIALS)
            preferences[Keys.LAST_SHOWN_VERSION] = 0
            preferences[Keys.SHOW_TUTORIAL] = true
        }
    }

    /**
     * Set whether to show tutorials
     */
    suspend fun setShowTutorial(show: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.SHOW_TUTORIAL] = show
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: TutorialPreferences? = null

        fun getInstance(context: Context): TutorialPreferences {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TutorialPreferences(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
}

/**
 * Extension function to get tutorial ID from TutorialType
 */
fun TutorialType.toTutorialId(): String {
    return when (this) {
        TutorialType.ONBOARDING -> "onboarding"
        TutorialType.NOTE_TAKING -> "note_taking"
        TutorialType.ORGANIZATION -> "organization"
        TutorialType.COLLABORATION -> "collaboration"
    }
}
