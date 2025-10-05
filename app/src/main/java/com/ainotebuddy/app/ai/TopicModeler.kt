package com.ainotebuddy.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced topic modeling and extraction system
 */
@Singleton
class TopicModeler @Inject constructor() {
    
    // Topic categories with associated keywords
    private val topicCategories = mapOf(
        "work" to listOf(
            "meeting", "project", "task", "deadline", "client", "business", "office", "team",
            "manager", "colleague", "presentation", "report", "email", "call", "conference",
            "budget", "revenue", "profit", "strategy", "planning", "development", "marketing"
        ),
        "personal" to listOf(
            "family", "friend", "home", "hobby", "health", "exercise", "vacation", "travel",
            "shopping", "cooking", "movie", "book", "music", "game", "weekend", "birthday",
            "anniversary", "relationship", "personal", "life", "happiness", "goal"
        ),
        "academic" to listOf(
            "study", "research", "paper", "thesis", "course", "university", "college", "school",
            "professor", "student", "exam", "assignment", "lecture", "library", "learning",
            "education", "knowledge", "science", "theory", "analysis", "experiment"
        ),
        "creative" to listOf(
            "idea", "design", "art", "music", "writing", "creative", "inspiration", "project",
            "sketch", "draft", "concept", "innovation", "imagination", "artistic", "visual",
            "story", "poem", "song", "painting", "photography", "craft", "creation"
        ),
        "technology" to listOf(
            "software", "app", "code", "programming", "computer", "internet", "website", "data",
            "algorithm", "system", "network", "security", "cloud", "mobile", "digital",
            "technology", "innovation", "development", "platform", "database", "api"
        ),
        "finance" to listOf(
            "money", "budget", "expense", "income", "investment", "savings", "bank", "loan",
            "credit", "debt", "payment", "cost", "price", "financial", "economy", "market",
            "stock", "portfolio", "insurance", "tax", "retirement", "wealth"
        ),
        "health" to listOf(
            "health", "doctor", "medical", "hospital", "medicine", "treatment", "therapy",
            "exercise", "fitness", "diet", "nutrition", "wellness", "mental", "physical",
            "symptom", "diagnosis", "recovery", "prevention", "healthcare", "clinic"
        ),
        "travel" to listOf(
            "travel", "trip", "vacation", "flight", "hotel", "destination", "journey", "tour",
            "adventure", "explore", "visit", "country", "city", "culture", "experience",
            "passport", "luggage", "booking", "itinerary", "sightseeing", "restaurant"
        ),
        "food" to listOf(
            "food", "recipe", "cooking", "restaurant", "meal", "dinner", "lunch", "breakfast",
            "ingredient", "dish", "cuisine", "flavor", "taste", "kitchen", "chef", "menu",
            "grocery", "shopping", "nutrition", "healthy", "delicious", "eating"
        ),
        "entertainment" to listOf(
            "movie", "film", "show", "series", "music", "concert", "game", "sport", "book",
            "reading", "theater", "performance", "entertainment", "fun", "leisure", "hobby",
            "festival", "event", "party", "celebration", "activity", "recreation"
        )
    )
    
    // Stop words to filter out
    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "from", "up", "about", "into", "through", "during", "before", "after",
        "above", "below", "between", "among", "under", "over", "is", "are", "was", "were",
        "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would",
        "could", "should", "may", "might", "must", "can", "this", "that", "these", "those",
        "i", "you", "he", "she", "it", "we", "they", "me", "him", "her", "us", "them",
        "my", "your", "his", "her", "its", "our", "their", "myself", "yourself", "himself",
        "herself", "itself", "ourselves", "yourselves", "themselves", "what", "which",
        "who", "whom", "whose", "where", "when", "why", "how", "all", "any", "both",
        "each", "few", "more", "most", "other", "some", "such", "no", "nor", "not",
        "only", "own", "same", "so", "than", "too", "very", "just", "now", "here",
        "there", "then", "once", "again", "also", "still", "well", "get", "go", "come",
        "see", "know", "think", "say", "tell", "ask", "give", "take", "make", "want",
        "need", "try", "use", "work", "find", "feel", "seem", "look", "become", "leave"
    )
    
    /**
     * Extract topics from text content
     */
    private data class FullTopicResult(
        val topic: String,
        val confidence: Float,
        val keywords: List<String>
    )

    suspend fun extractTopics(content: String): List<com.ainotebuddy.app.ai.TopicResult> = withContext(Dispatchers.IO) {
        if (content.isBlank()) {
            return@withContext emptyList()
        }
        
        val words = preprocessText(content)
        val phrases = extractPhrases(content)
        
        // Calculate topic scores
        val topicScores = calculateTopicScores(words, phrases)
        
        // Extract key phrases as potential topics
        val keyPhrases = extractKeyPhrases(words, content)
        
        // Combine category-based and phrase-based topics
        val categoryTopics = topicScores.entries
            .filter { it.value > 0.3f }
            .map { (topic, score) ->
                FullTopicResult(
                    topic = topic,
                    confidence = score,
                    keywords = getTopicKeywords(topic, words)
                )
            }
        
        val phraseTopics = keyPhrases
            .filter { it.confidence > 0.4f }
            .map { phrase ->
                FullTopicResult(
                    topic = phrase.phrase,
                    confidence = phrase.confidence,
                    keywords = phrase.phrase.split(" ")
                )
            }
        
        // Merge and rank topics
        val allTopics = (categoryTopics + phraseTopics)
            .distinctBy { it.topic }
            .sortedByDescending { it.confidence }
            .take(10)
            .map { com.ainotebuddy.app.ai.TopicResult(it.topic, it.confidence) }
        
        allTopics
    }
    
    /**
     * Extract topics from multiple texts in batch
     */
    suspend fun batchExtractTopics(contents: List<String>): List<List<com.ainotebuddy.app.ai.TopicResult>> = withContext(Dispatchers.IO) {
        contents.map { content ->
            extractTopics(content)
        }
    }
    
    /**
     * Find topic trends across multiple documents
     */
    suspend fun analyzeTopicTrends(
        documents: List<Pair<Long, String>>
    ): TopicTrends = withContext(Dispatchers.IO) {
        val allTopics = documents.map { (timestamp, content) ->
            timestamp to extractTopics(content)
        }
        
        // Calculate topic frequency over time
        val topicTimeline = mutableMapOf<String, MutableList<Long>>()
        
        allTopics.forEach { (timestamp, topics) ->
            topics.forEach { topic ->
                topicTimeline.getOrPut(topic.topic) { mutableListOf() }.add(timestamp)
            }
        }
        
        // Find trending topics
        val trendingTopics = findTrendingTopics(topicTimeline)
        val emergingTopics = findEmergingTopics(topicTimeline)
        val decliningTopics = findDecliningTopics(topicTimeline)
        
        TopicTrends(
            trendingTopics = trendingTopics,
            emergingTopics = emergingTopics,
            decliningTopics = decliningTopics,
            topicEvolution = calculateTopicEvolution(topicTimeline)
        )
    }
    
    private fun preprocessText(content: String): List<String> {
        return content.lowercase()
            .replace(Regex("[^a-zA-Z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 2 && it !in stopWords }
    }
    
    private fun extractPhrases(content: String): List<String> {
        val sentences = content.split(Regex("[.!?]+"))
        val phrases = mutableListOf<String>()
        
        sentences.forEach { sentence ->
            val words = sentence.trim().split(Regex("\\s+"))
            
            // Extract 2-3 word phrases
            for (i in 0 until words.size - 1) {
                if (i < words.size - 1) {
                    val phrase = "${words[i]} ${words[i + 1]}".lowercase()
                    if (isValidPhrase(phrase)) {
                        phrases.add(phrase)
                    }
                }
                
                if (i < words.size - 2) {
                    val phrase = "${words[i]} ${words[i + 1]} ${words[i + 2]}".lowercase()
                    if (isValidPhrase(phrase)) {
                        phrases.add(phrase)
                    }
                }
            }
        }
        
        return phrases
    }
    
    private fun isValidPhrase(phrase: String): Boolean {
        val words = phrase.split(" ")
        return words.all { it.length > 2 && it !in stopWords } && 
               words.size >= 2 && 
               phrase.length > 5
    }
    
    private fun calculateTopicScores(words: List<String>, phrases: List<String>): Map<String, Float> {
        val scores = mutableMapOf<String, Float>()
        
        topicCategories.forEach { (category, keywords) ->
            var score = 0f
            var matches = 0
            
            // Word-based scoring
            keywords.forEach { keyword ->
                val wordMatches = words.count { it.contains(keyword) || keyword.contains(it) }
                if (wordMatches > 0) {
                    score += wordMatches * 0.5f
                    matches++
                }
            }
            
            // Phrase-based scoring
            keywords.forEach { keyword ->
                val phraseMatches = phrases.count { it.contains(keyword) }
                if (phraseMatches > 0) {
                    score += phraseMatches * 0.8f
                    matches++
                }
            }
            
            // Normalize score
            if (matches > 0) {
                val normalizedScore = score / (words.size + phrases.size).toFloat()
                val confidenceBoost = min(matches.toFloat() / keywords.size, 0.5f)
                scores[category] = min(normalizedScore + confidenceBoost, 1f)
            }
        }
        
        return scores
    }
    
    private fun extractKeyPhrases(words: List<String>, content: String): List<KeyPhrase> {
        val phrases = extractPhrases(content)
        val phraseFrequency = phrases.groupingBy { it }.eachCount()
        
        return phraseFrequency.entries
            .filter { it.value >= 2 || it.key.split(" ").size >= 2 }
            .map { (phrase, frequency) ->
                val confidence = calculatePhraseConfidence(phrase, frequency, words.size)
                KeyPhrase(phrase, confidence)
            }
            .filter { it.confidence > 0.3f }
            .sortedByDescending { it.confidence }
            .take(5)
    }
    
    private fun calculatePhraseConfidence(phrase: String, frequency: Int, totalWords: Int): Float {
        val phraseWords = phrase.split(" ")
        val phraseLength = phraseWords.size
        
        // Base confidence from frequency
        var confidence = min(frequency.toFloat() / totalWords * 10, 0.6f)
        
        // Boost for longer phrases
        confidence += (phraseLength - 1) * 0.1f
        
        // Boost for non-common words
        val uncommonWords = phraseWords.count { it.length > 5 }
        confidence += uncommonWords * 0.1f
        
        return min(confidence, 1f)
    }
    
    private fun getTopicKeywords(topic: String, words: List<String>): List<String> {
        val topicKeywords = topicCategories[topic] ?: emptyList()
        
        return topicKeywords.filter { keyword ->
            words.any { word -> word.contains(keyword) || keyword.contains(word) }
        }.take(5)
    }
    
    private fun findTrendingTopics(topicTimeline: Map<String, List<Long>>): List<TrendingTopic> {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000)
        
        return topicTimeline.entries.mapNotNull { (topic, timestamps) ->
            val recentCount = timestamps.count { it > oneWeekAgo }
            val previousCount = timestamps.count { it in twoWeeksAgo..oneWeekAgo }
            
            if (recentCount > 0 && previousCount > 0) {
                val trendScore = recentCount.toFloat() / previousCount
                if (trendScore > 1.2f) {
                    TrendingTopic(topic, trendScore, recentCount)
                } else null
            } else null
        }.sortedByDescending { it.trendScore }
    }
    
    private fun findEmergingTopics(topicTimeline: Map<String, List<Long>>): List<String> {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        
        return topicTimeline.entries
            .filter { (_, timestamps) ->
                timestamps.all { it > oneWeekAgo } && timestamps.size >= 2
            }
            .map { it.key }
            .take(5)
    }
    
    private fun findDecliningTopics(topicTimeline: Map<String, List<Long>>): List<String> {
        val now = System.currentTimeMillis()
        val oneWeekAgo = now - (7 * 24 * 60 * 60 * 1000)
        val twoWeeksAgo = now - (14 * 24 * 60 * 60 * 1000)
        
        return topicTimeline.entries
            .filter { (_, timestamps) ->
                val recentCount = timestamps.count { it > oneWeekAgo }
                val previousCount = timestamps.count { it in twoWeeksAgo..oneWeekAgo }
                previousCount > 0 && recentCount < previousCount * 0.5f
            }
            .map { it.key }
            .take(5)
    }
    
    private fun calculateTopicEvolution(topicTimeline: Map<String, List<Long>>): Map<String, List<TopicDataPoint>> {
        val evolution = mutableMapOf<String, List<TopicDataPoint>>()
        
        topicTimeline.forEach { (topic, timestamps) ->
            val sortedTimestamps = timestamps.sorted()
            val dataPoints = mutableListOf<TopicDataPoint>()
            
            // Group by week
            val weeklyData = sortedTimestamps.groupBy { timestamp ->
                val weekStart = timestamp - (timestamp % (7 * 24 * 60 * 60 * 1000))
                weekStart
            }
            
            weeklyData.forEach { (week, weekTimestamps) ->
                dataPoints.add(TopicDataPoint(week, weekTimestamps.size))
            }
            
            evolution[topic] = dataPoints.sortedBy { it.timestamp }
        }
        
        return evolution
    }
    
    /**
     * Suggest related topics based on current topics
     */
    fun suggestRelatedTopics(currentTopics: List<String>): List<String> {
        val relatedTopics = mutableSetOf<String>()
        
        currentTopics.forEach { topic ->
            // Find category of current topic
            val category = topicCategories.entries.find { (_, keywords) ->
                keywords.any { keyword -> topic.contains(keyword, ignoreCase = true) }
            }?.key
            
            // Add other topics from same category
            category?.let { cat ->
                topicCategories[cat]?.forEach { keyword ->
                    if (!currentTopics.any { it.contains(keyword, ignoreCase = true) }) {
                        relatedTopics.add(keyword)
                    }
                }
            }
        }
        
        return relatedTopics.take(8).toList()
    }
}

// Data classes for topic modeling
data class KeyPhrase(
    val phrase: String,
    val confidence: Float
)

data class TopicTrends(
    val trendingTopics: List<TrendingTopic>,
    val emergingTopics: List<String>,
    val decliningTopics: List<String>,
    val topicEvolution: Map<String, List<TopicDataPoint>>
)

data class TrendingTopic(
    val topic: String,
    val trendScore: Float,
    val recentMentions: Int
)

data class TopicDataPoint(
    val timestamp: Long,
    val frequency: Int
)