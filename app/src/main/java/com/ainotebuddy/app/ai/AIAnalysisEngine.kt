package com.ainotebuddy.app.ai

import com.ainotebuddy.app.data.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simplified AI Analysis Engine for basic functionality
 */
@Singleton 
class AIAnalysisEngine @Inject constructor() {
    
    /**
     * Analyze a single note with basic AI processing
     */
    suspend fun analyzeNote(note: NoteEntity): AIAnalysisResult = withContext(Dispatchers.IO) {
        val content = "${note.title}\n${note.content}"
        
        AIAnalysisResult(
            sentiment = SentimentAnalyzer().analyzeSentiment(content),
            topics = extractTopicsBasic(content).map { it.topic }, // topics here are strings for compatibility with AIAnalysisResult
            entities = emptyList(),
            actionItems = extractActionItemsBasic(content).map { ActionItem(it, ActionPriority.MEDIUM) },
            keyPhrases = extractTopicsBasic(content).map { it.topic },
            insights = listOf(generateSummaryBasic(content)),
            contextualTags = emptyList(),
            confidence = 0.8f
        )
    }
    
    /**
     * Extract topics from query text
     */
    suspend fun extractTopicsFromQuery(query: String): List<TopicResult> {
        return extractTopicsBasic(query).map { TopicResult(it.topic, it.confidence) }
    }
    
    /**
     * Extract entities from query text  
     */
    suspend fun extractEntitiesFromQuery(query: String): List<EntityResult> {
        return EntityRecognizer().recognizeEntities(query)
    }
    
    /**
     * Generate content insights for multiple notes
     */
    // Simplified aggregation summary across notes to replace previously removed classes
    suspend fun generateContentSummary(notes: List<NoteEntity>): List<String> {
        val totalActionItems = notes.sumOf { extractActionItemsBasic("${it.title}\n${it.content}").size }
        val topTopics = notes.flatMap { extractTopicsBasic("${it.title}\n${it.content}") }
            .groupBy { it.topic }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        return listOf(
            "Total action items: $totalActionItems",
            "Top topics: ${topTopics.joinToString(", ")}"
        )
    }
    
    // Basic implementations
    private fun analyzeSentimentBasic(content: String): String {
        val positiveWords = listOf("good", "great", "excellent", "happy", "love", "amazing")
        val negativeWords = listOf("bad", "terrible", "awful", "hate", "sad", "horrible")
        
        val words = content.lowercase().split("\\s+".toRegex())
        val positiveCount = words.count { it in positiveWords }
        val negativeCount = words.count { it in negativeWords }
        
        return when {
            positiveCount > negativeCount -> "Positive"
            negativeCount > positiveCount -> "Negative"
            else -> "Neutral"
        }
    }
    
    internal fun extractTopicsBasic(content: String): List<TopicCandidate> {
        return content
            .split("\\s+".toRegex())
            .filter { it.length > 4 }
            .groupBy { it.lowercase() }
            .entries
            .sortedByDescending { it.value.size }
            .take(3)
            .map { TopicCandidate(it.key, it.value.size.toFloat() / 10f) }
    }

    internal data class TopicCandidate(val topic: String, val confidence: Float)
    
    internal fun extractActionItemsBasic(content: String): List<String> {
        val actionWords = listOf("todo", "need to", "should", "must", "remember to")
        return content.split("\n").filter { line ->
            actionWords.any { actionWord ->
                line.lowercase().contains(actionWord)
            }
        }.take(5)
    }
    
    private fun generateSummaryBasic(content: String): String {
        val sentences = content.split("[.!?]+".toRegex())
            .map { it.trim() }
            .filter { it.isNotBlank() }
        
        return when {
            sentences.isEmpty() -> "Empty content"
            sentences.size == 1 -> sentences.first()
            else -> sentences.take(2).joinToString(". ") + "..."
        }
    }
}

// Keep a single source of truth for AI models by reusing AIDataClasses
// This avoids duplicate declarations across the ai package.