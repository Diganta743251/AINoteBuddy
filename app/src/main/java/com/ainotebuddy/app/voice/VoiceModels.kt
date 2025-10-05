package com.ainotebuddy.app.voice

import kotlinx.serialization.Serializable

/**
 * Data models for voice processing and commands
 */

@Serializable
data class VoiceCommand(
    val id: String,
    val transcript: String,
    val confidence: Float,
    val timestamp: Long,
    val command: CommandType,
    val parameters: Map<String, String> = emptyMap()
)

@Serializable
enum class CommandType {
    CREATE_NOTE,
    EDIT_NOTE,
    DELETE_NOTE,
    SEARCH_NOTES,
    ADD_TAG,
    SET_REMINDER,
    DICTATE_TEXT,
    PLAY_AUDIO,
    STOP_RECORDING,
    START_RECORDING,
    SAVE_NOTE,
    OPEN_NOTE,
    LIST_NOTES,
    UNKNOWN
}

@Serializable
data class VoiceProcessingResult(
    val success: Boolean,
    val transcript: String,
    val confidence: Float,
    val commandRecognized: CommandType,
    val extractedData: VoiceExtractedData?,
    val errorMessage: String? = null,
    val processingTimeMs: Long
)

@Serializable
data class VoiceExtractedData(
    val noteTitle: String? = null,
    val noteContent: String? = null,
    val tags: List<String> = emptyList(),
    val category: String? = null,
    val reminderTime: Long? = null,
    val searchQuery: String? = null,
    val targetNoteId: Long? = null
)

@Serializable
data class SpeechRecognitionConfig(
    val language: String = "en-US",
    val enablePunctuation: Boolean = true,
    val enableWordConfidence: Boolean = true,
    val maxAlternatives: Int = 3,
    val timeoutMs: Long = 10000,
    val partialResults: Boolean = true
)

@Serializable
data class TextToSpeechConfig(
    val language: String = "en-US",
    val pitch: Float = 1.0f,
    val speechRate: Float = 1.0f,
    val voice: String? = null,
    val volume: Float = 1.0f
)

@Serializable
data class VoiceNote(
    val id: String,
    val originalAudioPath: String?,
    val transcript: String,
    val confidence: Float,
    val duration: Long,
    val createdAt: Long,
    val processedContent: ProcessedVoiceContent
)

@Serializable
data class ProcessedVoiceContent(
    val cleanedText: String,
    val extractedEntities: List<VoiceEntity>,
    val suggestedTitle: String,
    val detectedIntents: List<VoiceIntent>,
    val keyPhrases: List<String>
)

@Serializable
data class VoiceEntity(
    val text: String,
    val type: EntityType,
    val confidence: Float,
    val startIndex: Int,
    val endIndex: Int
)

@Serializable
enum class EntityType {
    PERSON,
    ORGANIZATION,
    LOCATION,
    DATE,
    TIME,
    PHONE,
    EMAIL,
    URL,
    NUMBER,
    TASK,
    REMINDER
}

@Serializable
data class VoiceIntent(
    val intent: IntentType,
    val confidence: Float,
    val parameters: Map<String, String>
)

@Serializable
enum class IntentType {
    CREATE_REMINDER,
    SCHEDULE_MEETING,
    ADD_CONTACT,
    MAKE_LIST,
    TAKE_NOTES,
    SEARCH_INFORMATION,
    SET_TIMER,
    PLAY_MUSIC,
    SEND_MESSAGE,
    MAKE_CALL
}

@Serializable
data class VoiceCommandHistory(
    val commands: List<VoiceCommand>,
    val totalCommands: Int,
    val successfulCommands: Int,
    val averageConfidence: Float,
    val mostUsedCommands: List<CommandType>,
    val timeRange: VoiceTimeRange
)

@Serializable
data class VoiceTimeRange(
    val startTime: Long,
    val endTime: Long,
    val durationMs: Long
)

@Serializable
data class VoiceSettings(
    val recognitionConfig: SpeechRecognitionConfig,
    val ttsConfig: TextToSpeechConfig,
    val enableWakeWord: Boolean = false,
    val wakeWord: String = "Hey Buddy",
    val enableContinuousListening: Boolean = false,
    val enableVoiceCommands: Boolean = true,
    val enableHapticFeedback: Boolean = true,
    val enableAudioFeedback: Boolean = true
)

@Serializable
data class VoiceAnalytics(
    val totalRecordings: Int,
    val totalDuration: Long,
    val averageRecordingLength: Long,
    val successRate: Float,
    val mostActiveHours: List<Int>,
    val languageDistribution: Map<String, Int>,
    val commandDistribution: Map<CommandType, Int>
)

@Serializable
data class VoiceQuality(
    val noiseLevel: Float,
    val clarity: Float,
    val volume: Float,
    val backgroundNoise: Boolean,
    val recommendation: String
)

@Serializable
data class VoiceProcessingPipeline(
    val stages: List<ProcessingStage>,
    val currentStage: ProcessingStage,
    val progress: Float,
    val estimatedTimeRemaining: Long
)

@Serializable
enum class ProcessingStage {
    AUDIO_CAPTURE,
    NOISE_REDUCTION,
    SPEECH_RECOGNITION,
    LANGUAGE_PROCESSING,
    INTENT_EXTRACTION,
    COMMAND_EXECUTION,
    RESPONSE_GENERATION,
    COMPLETED
}

@Serializable
data class VoiceError(
    val type: VoiceErrorType,
    val message: String,
    val code: Int,
    val recoverable: Boolean,
    val suggestion: String?
)

@Serializable
enum class VoiceErrorType {
    MICROPHONE_PERMISSION_DENIED,
    MICROPHONE_NOT_AVAILABLE,
    NETWORK_ERROR,
    SPEECH_NOT_DETECTED,
    LANGUAGE_NOT_SUPPORTED,
    PROCESSING_TIMEOUT,
    AUDIO_FORMAT_ERROR,
    STORAGE_ERROR,
    UNKNOWN_ERROR
}

// Voice command patterns for natural language processing
object VoiceCommandPatterns {
    val CREATE_NOTE_PATTERNS = listOf(
        "create (a )?new note",
        "make (a )?note",
        "start (a )?new note",
        "add (a )?note",
        "take (a )?note"
    )
    
    val EDIT_NOTE_PATTERNS = listOf(
        "edit (the )?note",
        "modify (the )?note",
        "change (the )?note",
        "update (the )?note"
    )
    
    val DELETE_NOTE_PATTERNS = listOf(
        "delete (the )?note",
        "remove (the )?note",
        "trash (the )?note"
    )
    
    val SEARCH_PATTERNS = listOf(
        "search for",
        "find",
        "look for",
        "show me"
    )
    
    val REMINDER_PATTERNS = listOf(
        "remind me",
        "set (a )?reminder",
        "create (a )?reminder"
    )
}

// Utility functions for voice processing
object VoiceUtils {
    fun extractCommandType(transcript: String): CommandType {
        val lowercaseTranscript = transcript.lowercase()
        
        return when {
            VoiceCommandPatterns.CREATE_NOTE_PATTERNS.any { pattern ->
                lowercaseTranscript.contains(Regex(pattern))
            } -> CommandType.CREATE_NOTE
            
            VoiceCommandPatterns.EDIT_NOTE_PATTERNS.any { pattern ->
                lowercaseTranscript.contains(Regex(pattern))
            } -> CommandType.EDIT_NOTE
            
            VoiceCommandPatterns.DELETE_NOTE_PATTERNS.any { pattern ->
                lowercaseTranscript.contains(Regex(pattern))
            } -> CommandType.DELETE_NOTE
            
            VoiceCommandPatterns.SEARCH_PATTERNS.any { pattern ->
                lowercaseTranscript.contains(Regex(pattern))
            } -> CommandType.SEARCH_NOTES
            
            VoiceCommandPatterns.REMINDER_PATTERNS.any { pattern ->
                lowercaseTranscript.contains(Regex(pattern))
            } -> CommandType.SET_REMINDER
            
            else -> CommandType.DICTATE_TEXT
        }
    }
    
    fun calculateConfidenceScore(
        recognitionConfidence: Float,
        commandMatchScore: Float,
        contextScore: Float
    ): Float {
        return (recognitionConfidence * 0.5f + commandMatchScore * 0.3f + contextScore * 0.2f)
            .coerceIn(0.0f, 1.0f)
    }
}