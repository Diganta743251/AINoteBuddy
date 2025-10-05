package com.ainotebuddy.app.voice.model

import androidx.compose.runtime.Immutable
import java.util.UUID

/**
 * Represents the current state of voice navigation
 */
@Immutable
data class VoiceNavigationState(
    val currentScreen: String = "",
    val currentNoteId: String? = null,
    val currentTarget: VoiceNavigationTarget? = null,
    val navigationHistory: List<VoiceNavigationTarget> = emptyList(),
    val lastNavigationTime: Long = 0,
    val sessionId: String = UUID.randomUUID().toString()
)

/**
 * Sealed class representing different navigation targets for voice commands
 */
sealed class VoiceNavigationTarget {
    object Home : VoiceNavigationTarget()
    object Back : VoiceNavigationTarget()
    object Settings : VoiceNavigationTarget()
    
    data class NoteDetail(val noteId: String) : VoiceNavigationTarget()
    data class CreateNote(val initialContent: String = "") : VoiceNavigationTarget()
    data class EditNote(val noteId: String, val contentToAdd: String) : VoiceNavigationTarget()
    data class DeleteNote(val noteId: String) : VoiceNavigationTarget()
    data class ArchiveNote(val noteId: String) : VoiceNavigationTarget()
    data class Search(val query: String) : VoiceNavigationTarget()
    data class SetReminder(val noteId: String, val reminderText: String) : VoiceNavigationTarget()
    data class AddTag(val noteId: String, val tagText: String) : VoiceNavigationTarget()
}

/**
 * Data class representing a voice navigation event
 */
data class VoiceNavigationEvent(
    val target: VoiceNavigationTarget,
    val timestamp: Long = System.currentTimeMillis(),
    val source: String = "voice_command",
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Data class for voice navigation statistics
 */
data class VoiceNavigationStats(
    val totalCommands: Int = 0,
    val successfulNavigations: Int = 0,
    val failedNavigations: Int = 0,
    val mostUsedCommand: String = "",
    val averageConfidence: Float = 0f,
    val lastUsed: Long = 0
)

/**
 * Data class for voice navigation settings
 */
data class VoiceNavigationSettings(
    val enabled: Boolean = true,
    val confirmDestructiveActions: Boolean = true,
    val readBackConfirmation: Boolean = true,
    val soundEffects: Boolean = true,
    val hapticFeedback: Boolean = true,
    val autoDismissDelayMs: Long = 3000,
    val preferredLanguage: String = "en-US"
)

/**
 * Data class for voice command suggestions based on current context
 */
data class VoiceCommandSuggestion(
    val command: String,
    val description: String,
    val iconResId: Int? = null,
    val confidence: Float = 0f,
    val requiresConfirmation: Boolean = false
)

/**
 * Data class representing the result of a voice navigation operation
 */
data class VoiceNavigationResult(
    val success: Boolean,
    val target: VoiceNavigationTarget,
    val message: String? = null,
    val error: Throwable? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Sealed class representing different types of voice navigation feedback
 */
sealed class VoiceNavigationFeedback {
    data class Success(val message: String) : VoiceNavigationFeedback()
    data class Error(val message: String) : VoiceNavigationFeedback()
    data class ConfirmationRequired(
        val message: String,
        val onConfirm: () -> Unit,
        val onDismiss: () -> Unit
    ) : VoiceNavigationFeedback()
    
    object Listening : VoiceNavigationFeedback()
    object Processing : VoiceNavigationFeedback()
    object Idle : VoiceNavigationFeedback()
}
