package com.ainotebuddy.app.preferences

import android.content.SharedPreferences

/**
 * Extension properties for voice command related preferences
 */
var SharedPreferences.isVoiceCommandsEnabled: Boolean
    get() = getBoolean("pref_voice_commands_enabled", true)
    set(value) = edit().putBoolean("pref_voice_commands_enabled", value).apply()

var SharedPreferences.isWakeWordEnabled: Boolean
    get() = getBoolean("pref_wake_word_enabled", true)
    set(value) = edit().putBoolean("pref_wake_word_enabled", value).apply()

var SharedPreferences.isAudioFeedbackEnabled: Boolean
    get() = getBoolean("pref_audio_feedback_enabled", true)
    set(value) = edit().putBoolean("pref_audio_feedback_enabled", value).apply()

var SharedPreferences.isVibrateOnCommand: Boolean
    get() = getBoolean("pref_vibrate_on_command", true)
    set(value) = edit().putBoolean("pref_vibrate_on_command", value).apply()

var SharedPreferences.isAutoPunctuationEnabled: Boolean
    get() = getBoolean("pref_auto_punctuation", true)
    set(value) = edit().putBoolean("pref_auto_punctuation", value).apply()

var SharedPreferences.isPrivacyModeEnabled: Boolean
    get() = getBoolean("pref_voice_privacy_mode", false)
    set(value) = edit().putBoolean("pref_voice_privacy_mode", value).apply()

var SharedPreferences.voiceCommandLanguage: String
    get() = getString("pref_voice_language", "en-US") ?: "en-US"
    set(value) = edit().putString("pref_voice_language", value).apply()

// Default values for voice command preferences
const val DEFAULT_VOICE_COMMAND_LANGUAGE = "en-US"
