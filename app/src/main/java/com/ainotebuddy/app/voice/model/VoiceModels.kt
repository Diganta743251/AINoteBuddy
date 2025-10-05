package com.ainotebuddy.app.voice.model

/**
 * Sealed class representing the result of voice recognition
 */
sealed class VoiceRecognitionResult {
    /** Listening for voice input has started */
    object ListeningStarted : VoiceRecognitionResult()
    
    /** Partial recognition results as the user speaks */
    data class PartialResult(val text: String, val confidence: Float) : VoiceRecognitionResult()
    
    /** Final recognition result */
    data class Result(val text: String, val confidence: Float, val command: VoiceCommand) : VoiceRecognitionResult()
    
    /** An error occurred during voice recognition */
    data class Error(val message: String, val errorCode: String? = null) : VoiceRecognitionResult()
    
    /** Voice recognition was cancelled */
    object Cancelled : VoiceRecognitionResult()
    
    /** No speech was detected */
    object NoMatch : VoiceRecognitionResult()
    
    /** Speech recognition is not available on the device */
    object NotAvailable : VoiceRecognitionResult()
    
    /** User needs to grant microphone permission */
    object PermissionRequired : VoiceRecognitionResult()
}

/**
 * Sealed class representing different types of voice commands
 */
sealed class VoiceCommand(val rawText: String, val confidence: Float) {
    // Note-related commands
    data class CreateNote(val content: String, val conf: Float) : VoiceCommand(content, conf)
    data class AddToNote(val content: String, val conf: Float) : VoiceCommand(content, conf)
    data class OpenNote(val noteTitle: String, val conf: Float) : VoiceCommand(noteTitle, conf)
    data class DeleteNote(val noteTitle: String, val conf: Float) : VoiceCommand(noteTitle, conf)
    data class ArchiveNote(val noteTitle: String, val conf: Float) : VoiceCommand(noteTitle, conf)
    
    // Search and organization
    data class Search(val query: String, val conf: Float) : VoiceCommand(query, conf)
    data class SetReminder(val reminderText: String, val conf: Float) : VoiceCommand(reminderText, conf)
    data class AddTag(val tagText: String, val conf: Float) : VoiceCommand(tagText, conf)
    
    // Navigation
    object GoHome : VoiceCommand("go home", 1.0f)
    object GoBack : VoiceCommand("go back", 1.0f)
    object OpenSettings : VoiceCommand("open settings", 1.0f)
    
    // System
    object Cancel : VoiceCommand("cancel", 1.0f)
    object Help : VoiceCommand("help", 1.0f)
    
    // Unknown command
    data class Unknown(val text: String, val conf: Float) : VoiceCommand(text, conf)
}

/**
 * Data class representing voice command recognition settings
 */
data class VoiceRecognitionSettings(
    val language: String = "en-US",
    val enablePartialResults: Boolean = true,
    val enableOffline: Boolean = true,
    val confirmationSound: Boolean = true,
    val showConfidence: Boolean = false,
    val autoStopAfterSilenceMs: Long = 2000,
    val minConfidence: Float = 0.4f
)

/**
 * Data class representing a voice command with its execution result
 */
data class ExecutedVoiceCommand(
    val command: VoiceCommand,
    val timestamp: Long = System.currentTimeMillis(),
    val success: Boolean = false,
    val result: Any? = null,
    val error: String? = null
)

/**
 * Data class for voice command statistics
 */
data class VoiceCommandStats(
    val totalCommands: Int = 0,
    val successfulCommands: Int = 0,
    val failedCommands: Int = 0,
    val mostUsedCommand: String = "",
    val averageConfidence: Float = 0f,
    val lastUsed: Long = 0
)

/**
 * Data class for voice command training data
 */
data class VoiceCommandTrainingData(
    val phrase: String,
    val commandType: String,
    val parameters: Map<String, String> = emptyMap(),
    val language: String = "en",
    val userId: String? = null
)

/**
 * Data class for voice command recognition result
 */
data class VoiceRecognition(
    val text: String,
    val confidence: Float,
    val isFinal: Boolean,
    val timestamp: Long = System.currentTimeMillis(),
    val alternatives: List<String> = emptyList()
)

/**
 * Data class for voice command execution context
 */
data class VoiceCommandContext(
    val currentScreen: String,
    val availableActions: List<String>,
    val previousCommand: VoiceCommand? = null,
    val sessionId: String = "",
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * Data class for voice command help information
 */
data class VoiceCommandHelp(
    val command: String,
    val description: String,
    val examples: List<String>,
    val availableInScreens: List<String>,
    val requiresParameters: Boolean = false
)
