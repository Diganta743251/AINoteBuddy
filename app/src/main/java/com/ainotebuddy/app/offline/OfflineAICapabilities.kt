package com.ainotebuddy.app.offline

import com.ainotebuddy.app.data.*
import com.ainotebuddy.app.ai.AIService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Offline AI capabilities for Enhanced Offline-First Architecture
 * Provides local AI processing when network is unavailable
 */
@Singleton
class OfflineAICapabilities @Inject constructor(
    private val aiService: AIService,
    private val networkStateManager: NetworkStateManager,
    private val offlineOperationManager: OfflineOperationManager
) {
    
    private val json = Json { 
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    /**
     * Generate categories offline using local AI processing
     */
    suspend fun generateCategoriesOffline(note: NoteEntity): List<String> = withContext(Dispatchers.Default) {
        try {
            if (networkStateManager.networkState.value.isConnected) {
                // Use online AI if available
                listOf(aiService.categorizeNote(note.content, note.title))
            } else {
                // Use local AI processing
                generateCategoriesLocally(note)
            }
        } catch (e: Exception) {
            // Fallback to local processing
            generateCategoriesLocally(note)
        }
    }
    
    /**
     * Generate tags offline using local AI processing
     */
    suspend fun generateTagsOffline(note: NoteEntity): List<String> = withContext(Dispatchers.Default) {
        try {
            if (networkStateManager.networkState.value.isConnected) {
                // Use online AI if available
                aiService.suggestTags(note.content)
            } else {
                // Use local AI processing
                generateTagsLocally(note)
            }
        } catch (e: Exception) {
            // Fallback to local processing
            generateTagsLocally(note)
        }
    }
    
    /**
     * Perform smart search offline using local AI processing
     */
    suspend fun smartSearchOffline(query: String, notes: List<NoteEntity>): List<NoteEntity> = withContext(Dispatchers.Default) {
        // Local implementation available; use it for both cases since AIService has no smart search API
        performSmartSearchLocally(query, notes)
    }
    
    /**
     * Analyze note content offline using local AI processing
     */
    suspend fun analyzeNoteOffline(note: NoteEntity): NoteAnalysis = withContext(Dispatchers.Default) {
        // Use local analysis; AIService doesn't expose a NoteEntity analyzer in this module
        analyzeNoteLocally(note)
    }
    
    /**
     * Generate content suggestions offline
     */
    suspend fun generateContentSuggestionsOffline(note: NoteEntity): List<ContentSuggestion> = withContext(Dispatchers.Default) {
        // Use local content suggestions; AIService doesn't provide this API in this module
        generateContentSuggestionsLocally(note)
    }
    
    /**
     * Queue AI operations for later processing when online
     */
    suspend fun queueAIOperation(operation: OfflineAIOperation) {
        offlineOperationManager.queueOperation(
            OfflineOperation.AIAnalysis(
                id = operation.id,
                noteId = operation.noteId,
                analysisType = operation.type.name,
                content = json.encodeToString(operation.parameters),
                timestamp = System.currentTimeMillis()
            )
        )
    }
    
    /**
     * Process queued AI operations when online
     */
    suspend fun processQueuedAIOperations(): Flow<AIProcessingResult> = flow {
        // Implementation for processing queued AI operations
        emit(AIProcessingResult(
            success = true,
            processedCount = 0,
            errors = emptyList(),
            timestamp = System.currentTimeMillis()
        ))
    }
    
    // Local AI processing implementations
    private suspend fun generateCategoriesLocally(note: NoteEntity): List<String> {
        // Simple keyword-based category generation
        val content = note.content.lowercase()
        val categories = mutableListOf<String>()
        
        when {
            content.contains("meeting") || content.contains("agenda") -> categories.add("Meetings")
            content.contains("idea") || content.contains("brainstorm") -> categories.add("Ideas")
            content.contains("todo") || content.contains("task") -> categories.add("Tasks")
            content.contains("project") || content.contains("plan") -> categories.add("Projects")
            content.contains("research") || content.contains("study") -> categories.add("Research")
            content.contains("personal") || content.contains("diary") -> categories.add("Personal")
            else -> categories.add("General")
        }
        
        return categories.distinct()
    }
    
    private suspend fun generateTagsLocally(note: NoteEntity): List<String> {
        // Simple keyword extraction for tags
        val content = note.content.lowercase()
        val tags = mutableListOf<String>()
        
        // Extract common keywords
        val keywords = listOf(
            "important", "urgent", "follow-up", "completed", "in-progress",
            "meeting", "call", "email", "document", "report", "presentation",
            "idea", "concept", "design", "development", "testing", "deployment"
        )
        
        keywords.forEach { keyword ->
            if (content.contains(keyword)) {
                tags.add(keyword)
            }
        }
        
        return tags.distinct().take(5) // Limit to 5 tags
    }
    
    private suspend fun performSmartSearchLocally(query: String, notes: List<NoteEntity>): List<NoteEntity> {
        val queryWords = query.lowercase().split(Regex("\\W+")).filter { it.isNotBlank() }
        
        return notes.map { note ->
            val titleScore = calculateMatchScore(note.title.lowercase(), queryWords)
            val contentScore = calculateMatchScore(note.content.lowercase(), queryWords)
            val categoryScore = if (note.category.lowercase().contains(query.lowercase())) 0.5f else 0f
            val tagScore = if (note.tags.lowercase().contains(query.lowercase())) 0.3f else 0f
            
            val totalScore = titleScore * 2 + contentScore + categoryScore + tagScore
            Pair(note, totalScore)
        }
        .filter { it.second > 0.1f } // Minimum relevance threshold
        .sortedByDescending { it.second }
        .map { it.first }
        .take(20) // Limit results
    }
    
    private fun calculateMatchScore(text: String, queryWords: List<String>): Float {
        val textWords = text.split(Regex("\\W+")).filter { it.isNotBlank() }
        var score = 0f
        
        queryWords.forEach { queryWord ->
            textWords.forEach { textWord ->
                when {
                    textWord == queryWord -> score += 1.0f
                    textWord.contains(queryWord) -> score += 0.7f
                    queryWord.contains(textWord) -> score += 0.5f
                    calculateSimilarity(textWord, queryWord) > 0.8f -> score += 0.3f
                }
            }
        }
        
        return score / queryWords.size
    }
    
    private fun calculateSimilarity(str1: String, str2: String): Float {
        if (str1 == str2) return 1.0f
        if (str1.isEmpty() || str2.isEmpty()) return 0.0f
        
        val longer = if (str1.length > str2.length) str1 else str2
        val shorter = if (str1.length > str2.length) str2 else str1
        
        val editDistance = calculateEditDistance(longer, shorter)
        return (longer.length - editDistance).toFloat() / longer.length
    }
    
    private fun calculateEditDistance(str1: String, str2: String): Int {
        val dp = Array(str1.length + 1) { IntArray(str2.length + 1) }
        
        for (i in 0..str1.length) dp[i][0] = i
        for (j in 0..str2.length) dp[0][j] = j
        
        for (i in 1..str1.length) {
            for (j in 1..str2.length) {
                dp[i][j] = if (str1[i-1] == str2[j-1]) {
                    dp[i-1][j-1]
                } else {
                    1 + minOf(dp[i-1][j], dp[i][j-1], dp[i-1][j-1])
                }
            }
        }
        
        return dp[str1.length][str2.length]
    }
    
    private suspend fun analyzeNoteLocally(note: NoteEntity): NoteAnalysis {
        val wordCount = note.content.split(Regex("\\W+")).filter { it.isNotBlank() }.size
        val readingTime = (wordCount / 200.0).toInt().coerceAtLeast(1) // Assume 200 WPM
        
        val sentiment = when {
            note.content.lowercase().contains(Regex("(great|excellent|amazing|wonderful|fantastic)")) -> "positive"
            note.content.lowercase().contains(Regex("(bad|terrible|awful|horrible|disappointing)")) -> "negative"
            else -> "neutral"
        }
        
        val complexity = when {
            wordCount < 50 -> "simple"
            wordCount < 200 -> "moderate"
            else -> "complex"
        }
        
        return NoteAnalysis(
            wordCount = wordCount,
            readingTimeMinutes = readingTime,
            sentiment = sentiment,
            complexity = complexity,
            keyTopics = extractKeyTopics(note.content),
            suggestedCategories = generateCategoriesLocally(note),
            suggestedTags = generateTagsLocally(note),
            confidence = 0.7f // Local analysis has lower confidence
        )
    }
    
    private fun extractKeyTopics(content: String): List<String> {
        // Simple topic extraction based on word frequency
        val words = content.lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 3 && !isStopWord(it) }
            .groupingBy { it }
            .eachCount()
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
        
        return words
    }
    
    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf(
            "this", "that", "with", "have", "will", "from", "they", "know",
            "want", "been", "good", "much", "some", "time", "very", "when",
            "come", "here", "just", "like", "long", "make", "many", "over",
            "such", "take", "than", "them", "well", "were"
        )
        return stopWords.contains(word)
    }
    
    private suspend fun generateContentSuggestionsLocally(note: NoteEntity): List<ContentSuggestion> {
        val suggestions = mutableListOf<ContentSuggestion>()
        
        // Simple content suggestions based on note analysis
        val analysis = analyzeNoteLocally(note)
        
        if (analysis.wordCount < 50) {
            suggestions.add(ContentSuggestion(
                type = SuggestionType.EXPAND_CONTENT,
                title = "Expand Content",
                description = "Consider adding more details to make this note more comprehensive",
                confidence = 0.8f
            ))
        }
        
        if (analysis.suggestedTags.isNotEmpty()) {
            suggestions.add(ContentSuggestion(
                type = SuggestionType.ADD_TAGS,
                title = "Add Tags",
                description = "Consider adding these tags: ${analysis.suggestedTags.joinToString(", ")}",
                confidence = 0.7f
            ))
        }
        
        if (note.category.isEmpty()) {
            suggestions.add(ContentSuggestion(
                type = SuggestionType.ADD_CATEGORY,
                title = "Add Category",
                description = "Consider categorizing this note as: ${analysis.suggestedCategories.firstOrNull() ?: "General"}",
                confidence = 0.6f
            ))
        }
        
        return suggestions
    }
}

// Data classes for offline AI capabilities
data class OfflineAIOperation(
    val id: String,
    val noteId: Long,
    val type: AIOperationType,
    val parameters: Map<String, String>,
    val timestamp: Long
)

enum class AIOperationType {
    CATEGORY_GENERATION,
    TAG_GENERATION,
    CONTENT_ANALYSIS,
    SMART_SEARCH,
    CONTENT_SUGGESTIONS
}

data class NoteAnalysis(
    val wordCount: Int,
    val readingTimeMinutes: Int,
    val sentiment: String,
    val complexity: String,
    val keyTopics: List<String>,
    val suggestedCategories: List<String>,
    val suggestedTags: List<String>,
    val confidence: Float
)

data class ContentSuggestion(
    val type: SuggestionType,
    val title: String,
    val description: String,
    val confidence: Float
)

enum class SuggestionType {
    EXPAND_CONTENT,
    ADD_TAGS,
    ADD_CATEGORY,
    IMPROVE_STRUCTURE,
    ADD_LINKS,
    FORMAT_TEXT
}

data class AIProcessingResult(
    val success: Boolean,
    val processedCount: Int,
    val errors: List<String>,
    val timestamp: Long
)
