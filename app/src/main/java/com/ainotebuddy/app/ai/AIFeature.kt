package com.ainotebuddy.app.ai

import androidx.compose.runtime.Immutable
import com.ainotebuddy.app.data.NoteEntity

/**
 * Sealed class representing different types of AI features
 */
sealed class AIFeature {
    object ContextualSuggestions : AIFeature()
    object VoiceCommands : AIFeature()
    object AIGeneratedTags : AIFeature()
    object SentimentAnalysis : AIFeature()
}

/**
 * Data class representing an AI suggestion
 */
@Immutable
// Keep as a distinct UI-level suggestion to avoid clashing with core AIEngine suggestion
// Rename to UISuggestion to prevent redeclaration with com.ainotebuddy.app.ai.AISuggestion
// Note: Update usages accordingly if referenced elsewhere in UI layer.
data class UISuggestion(
    val id: String,
    val type: SuggestionType,
    val content: String,
    val confidence: Float,
    val metadata: Map<String, String> = emptyMap()
)





/**
 * Interface for AI service providers
 */
interface AIServiceProvider {
    suspend fun generateSuggestions(note: NoteEntity, context: String? = null): List<AISuggestion>
    suspend fun analyzeSentiment(note: NoteEntity): SentimentAnalysisResult
    suspend fun generateTags(note: NoteEntity, existingTags: List<String> = emptyList()): List<String>
    suspend fun processVoiceCommand(command: String): VoiceCommandResult
    
    /**
     * Process natural language query and return structured data
     */
    suspend fun processNaturalLanguageQuery(query: String): NLQueryResult
}

/**
 * Result of a voice command processing
 */
data class VoiceCommandResult(
    val success: Boolean,
    val commandType: CommandType,
    val parameters: Map<String, Any> = emptyMap(),
    val message: String? = null
) {
    enum class CommandType {
        CREATE_NOTE,
        SEARCH_NOTES,
        EDIT_NOTE,
        DELETE_NOTE,
        SET_REMINDER,
        SHARE_NOTE,
        UNDO,
        REDO,
        UNKNOWN
    }
}

/**
 * Result of natural language query processing
 */
data class NLQueryResult(
    val intent: String,
    val entities: Map<String, String>,
    val confidence: Float,
    val rawResponse: String? = null
)

/**
 * Data class representing a voice command
 */
data class VoiceCommand(
    val id: String,
    val command: String,
    val description: String,
    val example: String
)
