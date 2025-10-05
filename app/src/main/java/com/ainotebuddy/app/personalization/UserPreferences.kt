package com.ainotebuddy.app.personalization

import javax.inject.Inject
import javax.inject.Singleton

/**
 * User preferences for personalization
 */
@Singleton
class UserPreferences @Inject constructor() {
    
    data class VoicePreferences(
        val preferredLanguage: String = "en-US",
        val preferredVoiceSpeed: Float = 1.0f,
        val autoTranscriptionEnabled: Boolean = true,
        val voiceCommandsEnabled: Boolean = true
    )
    
    private var _voicePreferences = VoicePreferences()
    val voicePreferences: VoicePreferences get() = _voicePreferences
    
    fun updateVoicePreferences(preferences: VoicePreferences) {
        _voicePreferences = preferences
    }
}