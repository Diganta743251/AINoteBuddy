package com.ainotebuddy.app.ai

import com.ainotebuddy.app.data.NoteEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced AI Service with advanced features for smart note processing
 */
@Singleton
class EnhancedAIService @Inject constructor(
    private val aiService: AIService,
    private val sentimentAnalyzer: SentimentAnalyzer
) {
    
    /**
     * Comprehensive note analysis that combines multiple AI features
     */
    suspend fun performComprehensiveAnalysis(note: NoteEntity): ComprehensiveAnalysisResult {
        return withContext(Dispatchers.Default) {
            // Perform sentiment analysis
            val sentimentResult = sentimentAnalyzer.analyzeSentiment(note.content)
            
            // Extract key information
            val keyPhrases = extractKeyPhrases(note.content)
            val actionItems = extractActionItems(note.content)
            val entities = extractNamedEntities(note.content)
            val tags = generateSmartTags(note.content, note.title)
            val category = aiService.categorizeNote(note.content, note.title)
            val readingTime = calculateReadingTime(note.content)
            val complexity = assessComplexity(note.content)
            
            ComprehensiveAnalysisResult(
                noteId = note.id.toString(),
                sentiment = sentimentResult,
                keyPhrases = keyPhrases,
                actionItems = actionItems,
                entities = entities,
                recommendedTags = tags,
                suggestedCategory = category,
                readingTimeMinutes = readingTime,
                complexityScore = complexity,
                improvementSuggestions = generateImprovementSuggestions(note, sentimentResult, complexity)
            )
        }
    }
    
    /**
     * Extract key phrases from note content
     */
    private fun extractKeyPhrases(content: String): List<String> {
        val stopWords = setOf("the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by")
        val words = content.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 3 && !stopWords.contains(it) }
        
        // Simple frequency-based key phrase extraction
        val wordCounts = words.groupingBy { it }.eachCount()
        return wordCounts.entries
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
    }
    
    /**
     * Extract potential action items from content
     */
    private fun extractActionItems(content: String): List<String> {
        val actionWords = listOf("todo", "task", "action", "need to", "should", "must", "have to", "call", "email", "buy", "create", "update", "fix", "complete")
        val sentences = content.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        
        return sentences.filter { sentence ->
            actionWords.any { sentence.lowercase().contains(it) }
        }.take(5)
    }
    
    /**
     * Extract named entities (simplified implementation)
     */
    private fun extractNamedEntities(content: String): List<EnhancedEntity> {
        val entities = mutableListOf<EnhancedEntity>()
        
        // Simple pattern matching for common entities
        val namePattern = Regex("\\b[A-Z][a-z]+ [A-Z][a-z]+\\b")
        val companyPattern = Regex("\\b[A-Z][a-z]+ (?:Inc|Corp|LLC|Ltd)\\b")
        val emailPattern = Regex("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b")
        val phonePattern = Regex("\\b\\d{3}[-.]?\\d{3}[-.]?\\d{4}\\b")
        
        namePattern.findAll(content).forEach { match ->
            entities.add(EnhancedEntity(
                text = match.value,
                type = EntityType.PERSON,
                confidence = 0.8f,
                startOffset = match.range.first,
                endOffset = match.range.last + 1
            ))
        }
        
        companyPattern.findAll(content).forEach { match ->
            entities.add(EnhancedEntity(
                text = match.value,
                type = EntityType.ORGANIZATION,
                confidence = 0.7f,
                startOffset = match.range.first,
                endOffset = match.range.last + 1
            ))
        }
        
        emailPattern.findAll(content).forEach { match ->
            entities.add(EnhancedEntity(
                text = match.value,
                type = EntityType.EMAIL,
                confidence = 0.9f,
                startOffset = match.range.first,
                endOffset = match.range.last + 1
            ))
        }
        
        phonePattern.findAll(content).forEach { match ->
            entities.add(EnhancedEntity(
                text = match.value,
                type = EntityType.PHONE,
                confidence = 0.8f,
                startOffset = match.range.first,
                endOffset = match.range.last + 1
            ))
        }
        
        return entities
    }
    
    /**
     * Generate smart tags based on content analysis
     */
    private fun generateSmartTags(content: String, title: String): List<String> {
        val tags = mutableSetOf<String>()
        val fullText = "$title $content".lowercase()
        
        // Technology tags
        val techKeywords = mapOf(
            "programming" to listOf("code", "programming", "software", "development", "algorithm"),
            "meeting" to listOf("meeting", "discussion", "agenda", "minutes"),
            "project" to listOf("project", "deadline", "milestone", "task"),
            "research" to listOf("research", "study", "analysis", "investigation"),
            "personal" to listOf("personal", "diary", "journal", "thoughts"),
            "finance" to listOf("budget", "money", "expense", "income", "financial"),
            "health" to listOf("health", "fitness", "exercise", "medical", "doctor"),
            "travel" to listOf("travel", "trip", "vacation", "flight", "hotel")
        )
        
        techKeywords.forEach { (tag, keywords) ->
            if (keywords.any { fullText.contains(it) }) {
                tags.add(tag)
            }
        }
        
        return tags.toList()
    }
    
    /**
     * Calculate estimated reading time in minutes
     */
    private fun calculateReadingTime(content: String): Int {
        val wordsPerMinute = 250
        val wordCount = content.split(Regex("\\s+")).size
        return kotlin.math.max(1, (wordCount / wordsPerMinute))
    }
    
    /**
     * Assess content complexity on a scale of 1-10
     */
    private fun assessComplexity(content: String): Float {
        val sentences = content.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }
        val avgSentenceLength = if (sentences.isNotEmpty()) {
            content.split(Regex("\\s+")).size / sentences.size
        } else 1
        
        val longWords = content.split(Regex("\\s+")).count { it.length > 6 }
        val totalWords = content.split(Regex("\\s+")).size
        val longWordRatio = if (totalWords > 0) longWords.toFloat() / totalWords else 0f
        
        // Simple complexity score (1-10)
        val complexityScore = (avgSentenceLength * 0.1f + longWordRatio * 5f).coerceIn(1f, 10f)
        return complexityScore
    }
    
    /**
     * Generate improvement suggestions based on analysis
     */
    private fun generateImprovementSuggestions(
        note: NoteEntity,
        sentiment: SentimentResult,
        complexity: Float
    ): List<String> {
        val suggestions = mutableListOf<String>()
        
        if (note.content.length < 100) {
            suggestions.add("Consider adding more details to make this note more comprehensive")
        }
        
        if (complexity > 7) {
            suggestions.add("This note is quite complex. Consider breaking it into smaller sections")
        }
        
        if (sentiment.sentiment == Sentiment.NEGATIVE && sentiment.confidence > 0.7f) {
            suggestions.add("This note has negative sentiment. Consider adding positive action items")
        }
        
        if (note.title.length < 10) {
            suggestions.add("Consider making the title more descriptive")
        }
        
        if (note.tags.isNullOrEmpty()) {
            suggestions.add("Adding tags will help organize and find this note later")
        }
        
        return suggestions
    }
}

/**
 * Comprehensive analysis result containing multiple AI insights
 */
data class ComprehensiveAnalysisResult(
    val noteId: String,
    val sentiment: SentimentResult,
    val keyPhrases: List<String>,
    val actionItems: List<String>,
    val entities: List<EnhancedEntity>,
    val recommendedTags: List<String>,
    val suggestedCategory: String,
    val readingTimeMinutes: Int,
    val complexityScore: Float,
    val improvementSuggestions: List<String>
)

// Use core EntityType defined in AIDataClasses to avoid duplication
// and rename this Entity to EnhancedEntity to avoid collision.

data class EnhancedEntity(
    val text: String,
    val type: EntityType,
    val confidence: Float,
    val startOffset: Int,
    val endOffset: Int,
    val metadata: Map<String, String> = emptyMap()
)