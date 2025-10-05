package com.ainotebuddy.app.ai

import com.ainotebuddy.app.data.NoteEntity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of AIServiceProvider interface
 */
@Singleton
class AIServiceProviderImpl @Inject constructor(
    private val aiService: AIService
) : AIServiceProvider { // Implements interface in AIFeature.kt
    
    override suspend fun generateSuggestions(note: NoteEntity, context: String?): List<AISuggestion> {
        return try {
            // Convert NoteEntity to the format expected by AIService
            val suggestions = aiService.generateContextualSuggestions(note.content, context)
            
            // Convert to AISuggestion format
            suggestions.mapNotNull { suggestion ->
                when (suggestion) {
                    is ContextualSuggestion.ContentSuggestion -> {
                        AISuggestion(
                            id = "content_${System.currentTimeMillis()}",
                            type = SuggestionType.CONTENT_IMPROVEMENT,
                            title = "Content Suggestion",
                            description = suggestion.suggestion,
                            confidence = 0.8f,
                            metadata = mapOf(
                                "range_start" to suggestion.range.first.toString(),
                                "range_end" to suggestion.range.last.toString(),
                                "suggestion_type" to suggestion.type.name
                            )
                        )
                    }
                    is ContextualSuggestion.TagSuggestion -> {
                        AISuggestion(
                            id = "tags_${System.currentTimeMillis()}",
                            type = SuggestionType.TAGGING,
                            title = "Tag Suggestions",
                            description = "Suggested tags: ${suggestion.tags.joinToString(", ")}",
                            confidence = 0.9f,
                            metadata = mapOf("suggested_tags" to suggestion.tags.joinToString(","))
                        )
                    }
                    // CategorySuggestion no longer exists in ContextualSuggestion; skip or handle via tags/organization if added in future
                    else -> null
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    // analyzeNote is now provided by AIEngineImpl. Keep a lightweight delegating implementation for compatibility.
    suspend fun analyzeNote(note: NoteEntity): AIAnalysisResult {
        return try {
            // Delegate to a simple built-in analysis to avoid circular dependency
            val sentiment = SentimentResult(Sentiment.NEUTRAL, 0.5f, 0.3f, 0.2f, 0.5f)
            val topics = note.content.split(" ").filter { it.length > 3 }.take(5)
            AIAnalysisResult(
                sentiment = sentiment,
                topics = topics,
                entities = emptyList(),
                actionItems = emptyList(),
                keyPhrases = topics, // reuse as simple key phrases
                insights = listOf("Analysis available in Insights screen"),
                contextualTags = emptyList(),
                confidence = 0.6f
            )
        } catch (e: Exception) {
            AIAnalysisResult(
                sentiment = SentimentResult(Sentiment.NEUTRAL, 0.5f, 0.3f, 0.2f, 0.5f),
                topics = emptyList(),
                entities = emptyList(),
                actionItems = emptyList(),
                keyPhrases = emptyList(),
                insights = emptyList(),
                contextualTags = emptyList(),
                confidence = 0.0f
            )
        }
    }
        
        override suspend fun analyzeSentiment(note: NoteEntity): SentimentAnalysisResult {
        return aiService.analyzeSentiment(note.content)
    }
        
    override suspend fun generateTags(note: NoteEntity, existingTags: List<String>): List<String> {
        val tags = aiService.suggestTags(note.content)
        return tags.filterNot { it in existingTags }.distinct().take(5)
    }
    
    override suspend fun processVoiceCommand(command: String): VoiceCommandResult {
        val type = when {
            command.contains("create", ignoreCase = true) && command.contains("note", ignoreCase = true) -> VoiceCommandResult.CommandType.CREATE_NOTE
            command.contains("search", ignoreCase = true) -> VoiceCommandResult.CommandType.SEARCH_NOTES
            command.contains("delete", ignoreCase = true) -> VoiceCommandResult.CommandType.DELETE_NOTE
            command.contains("reminder", ignoreCase = true) -> VoiceCommandResult.CommandType.SET_REMINDER
            else -> VoiceCommandResult.CommandType.UNKNOWN
        }
        return VoiceCommandResult(success = true, commandType = type, message = "Processed")
    }
    
    override suspend fun processNaturalLanguageQuery(query: String): NLQueryResult {
        val intent = when {
            query.contains("find", ignoreCase = true) -> "search"
            query.contains("create", ignoreCase = true) -> "create"
            else -> "unknown"
        }
        return NLQueryResult(intent = intent, entities = emptyMap(), confidence = 0.5f)
    }
}