package com.ainotebuddy.app.ai

import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.WritingInsights
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.math.min
import java.util.*
import kotlin.collections.HashMap
import javax.inject.Inject
import javax.inject.Singleton

// Data class for contextual suggestion
sealed class ContextualSuggestion {
    data class TagSuggestion(val tags: List<String>) : ContextualSuggestion()
    data class RelatedNoteSuggestion(val noteId: String, val title: String, val similarity: Float) : ContextualSuggestion()
    data class ContentSuggestion(val range: IntRange, val suggestion: String, val type: AISuggestionType) : ContextualSuggestion()
    data class ActionSuggestion(val action: String, val description: String) : ContextualSuggestion()
}

// Use a distinct type name to avoid conflicts with other SuggestionType enums
enum class AISuggestionType {
    CONTENT_IMPROVEMENT, TAGGING, ORGANIZATION, RELATED_NOTE, PRODUCTIVITY, FORMATTING, COMPLETION
}

data class SentimentAnalysisResult(
    val sentiment: Sentiment,
    val confidence: Float,
    val keyPhrases: List<String>,
    val emotions: Map<String, Float>
)

@Singleton
class AIService @Inject constructor() {
    
    /**
     * Automatically categorize a note based on its content
     */
    suspend fun categorizeNote(content: String, title: String): String {
        return withContext(Dispatchers.Default) {
            // Simple categorization based on keywords
            val lowerContent = "$title $content".lowercase()
            
            when {
                lowerContent.contains("meeting") || lowerContent.contains("agenda") -> "Work"
                lowerContent.contains("recipe") || lowerContent.contains("cooking") -> "Cooking"
                lowerContent.contains("todo") || lowerContent.contains("task") -> "Tasks"
                lowerContent.contains("idea") || lowerContent.contains("brainstorm") -> "Ideas"
                lowerContent.contains("journal") || lowerContent.contains("diary") -> "Personal"
                else -> "General"
            }
        }
    }
    
    /**
     * Generate contextual suggestions for a note
     */
    suspend fun generateContextualSuggestions(content: String, context: String? = null): List<ContextualSuggestion> {
        return withContext(Dispatchers.Default) {
            val suggestions = mutableListOf<ContextualSuggestion>()
            
            // Tag suggestions
            val tags = suggestTags(content)
            if (tags.isNotEmpty()) {
                suggestions.add(ContextualSuggestion.TagSuggestion(tags))
            }
            
            // Content suggestions based on simple rules
            if (content.length < 50) {
                suggestions.add(
                    ContextualSuggestion.ContentSuggestion(
                        range = 0..content.length,
                        suggestion = "Consider adding more details to make this note more comprehensive",
                        type = AISuggestionType.COMPLETION
                    )
                )
            }
            
            suggestions
        }
    }
    
    /**
     * Suggest tags based on note content
     */
    suspend fun suggestTags(content: String): List<String> {
        return withContext(Dispatchers.Default) {
            val tags = mutableSetOf<String>()
            val lowerContent = content.lowercase()
            
            // Simple keyword-based tagging
            val tagMap = mapOf(
                "meeting" to listOf("work", "meeting"),
                "recipe" to listOf("cooking", "food"),
                "todo" to listOf("tasks", "productivity"),
                "idea" to listOf("brainstorm", "creative"),
                "journal" to listOf("personal", "reflection"),
                "project" to listOf("work", "project"),
                "book" to listOf("reading", "books"),
                "travel" to listOf("travel", "vacation")
            )
            
            tagMap.forEach { (keyword, suggestedTags) ->
                if (lowerContent.contains(keyword)) {
                    tags.addAll(suggestedTags)
                }
            }
            
            tags.take(5).toList() // Limit to 5 tags
        }
    }
    
    /**
     * Analyze sentiment of note content
     */
    suspend fun analyzeSentiment(content: String): SentimentAnalysisResult {
        return withContext(Dispatchers.Default) {
            // Simple sentiment analysis based on keywords
            val lowerContent = content.lowercase()
            
            val positiveWords = listOf("happy", "great", "awesome", "good", "excellent", "love", "wonderful", "amazing")
            val negativeWords = listOf("sad", "bad", "terrible", "awful", "hate", "horrible", "angry", "frustrated")
            
            val positiveCount = positiveWords.count { lowerContent.contains(it) }
            val negativeCount = negativeWords.count { lowerContent.contains(it) }
            
            val sentiment = when {
                positiveCount > negativeCount -> Sentiment.POSITIVE
                negativeCount > positiveCount -> Sentiment.NEGATIVE
                // If both positive and negative are present in equal measure, treat as NEUTRAL
                positiveCount == negativeCount && positiveCount > 0 -> Sentiment.NEUTRAL
                else -> Sentiment.NEUTRAL
            }
            
            val confidence = if (positiveCount + negativeCount > 0) {
                kotlin.math.abs(positiveCount - negativeCount).toFloat() / (positiveCount + negativeCount)
            } else {
                0.5f
            }
            
            SentimentAnalysisResult(
                sentiment = sentiment,
                confidence = confidence,
                keyPhrases = extractKeyPhrases(content),
                emotions = mapOf(
                    "joy" to if (positiveCount > 0) positiveCount.toFloat() / 10 else 0f,
                    "sadness" to if (negativeCount > 0) negativeCount.toFloat() / 10 else 0f
                )
            )
        }
    }
    
    private fun extractKeyPhrases(content: String): List<String> {
        return content.split(".")
            .filter { it.length > 10 }
            .map { it.trim().take(30) + "..." }
            .take(3)
    }

    // Bridging APIs to align with existing call sites in the app

    /** Alias for categorizeNote with (title, content) signature */
    suspend fun suggestCategory(title: String, content: String): String {
        return categorizeNote(content, title)
    }

    /** Overload suggestTags that accepts title + content */
    suspend fun suggestTags(title: String, content: String): List<String> {
        return suggestTags("$title\n$content")
    }

    /** Word count helper */
    suspend fun countWords(content: String): Int = withContext(Dispatchers.Default) {
        content.split(Regex("\\W+")).count { it.isNotBlank() }
    }

    /** estimated reading time in minutes @ ~200 wpm */
    suspend fun calculateReadingTime(content: String): Int = withContext(Dispatchers.Default) {
        val wc = countWords(content)
        kotlin.math.max(1, wc / 200)
    }

    /** Stub translation - returns original for offline mode */
    suspend fun translateText(text: String, targetLanguage: String): String = withContext(Dispatchers.Default) {
        text
    }

    /** Generate a short summary */
    suspend fun generateSummary(text: String): String = withContext(Dispatchers.Default) {
        if (text.length <= 120) text else text.take(120) + "..."
    }

    /** Simple writing improvement suggestions */
    suspend fun suggestImprovements(text: String): List<String> = withContext(Dispatchers.Default) {
        val suggestions = mutableListOf<String>()
        if (text.length < 80) suggestions.add("Consider adding more details to the note")
        if (!text.contains(Regex("[.!?]"))) suggestions.add("Add punctuation for readability")
        suggestions
    }

    /** Basic writing analysis returning repository.WritingInsights */
    suspend fun analyzeWriting(content: String): WritingInsights = withContext(Dispatchers.Default) {
        val words = content.split(Regex("\\W+")).filter { it.isNotBlank() }
        val longWords = words.count { it.length > 6 }
        val readability = if (words.isNotEmpty()) 1.0 - (longWords.toDouble() / words.size) else 0.0
        val sentiment = when {
            content.contains(Regex("(great|excellent|amazing|wonderful|fantastic)", RegexOption.IGNORE_CASE)) -> "positive"
            content.contains(Regex("(bad|terrible|awful|horrible|disappointing)", RegexOption.IGNORE_CASE)) -> "negative"
            else -> "neutral"
        }
        WritingInsights(
            readabilityScore = readability,
            sentiment = sentiment,
            keyTopics = emptyList(),
            suggestions = emptyList()
        )
    }
}