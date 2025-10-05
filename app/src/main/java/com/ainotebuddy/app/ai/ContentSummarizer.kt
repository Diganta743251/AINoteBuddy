package com.ainotebuddy.app.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Advanced content summarization system using extractive and abstractive techniques
 */
@Singleton
class ContentSummarizer @Inject constructor() {
    
    // Stop words for better sentence scoring
    private val stopWords = setOf(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
        "by", "from", "up", "about", "into", "through", "during", "before", "after",
        "above", "below", "between", "among", "under", "over", "is", "are", "was", "were",
        "be", "been", "being", "have", "has", "had", "do", "does", "did", "will", "would",
        "could", "should", "may", "might", "must", "can", "this", "that", "these", "those"
    )
    
    // Important keywords that boost sentence scores
    private val importantKeywords = setOf(
        "important", "crucial", "key", "main", "primary", "essential", "critical", "vital",
        "significant", "major", "fundamental", "core", "central", "principal", "basic",
        "conclusion", "result", "outcome", "finding", "discovery", "insight", "summary",
        "decision", "action", "plan", "goal", "objective", "target", "deadline", "urgent"
    )
    
    /**
     * Generate summary of text content
     */
    suspend fun generateSummary(
        content: String,
        maxSentences: Int = 3,
        compressionRatio: Float = 0.3f
    ): String = withContext(Dispatchers.IO) {
        if (content.isBlank() || content.length < 100) {
            return@withContext content.take(100)
        }
        
        val sentences = splitIntoSentences(content)
        if (sentences.size <= maxSentences) {
            return@withContext content
        }
        
        // Calculate sentence scores
        val sentenceScores = calculateSentenceScores(sentences, content)
        
        // Select top sentences
        val targetSentenceCount = maxOf(1, min(maxSentences, (sentences.size * compressionRatio).toInt()))
        val topSentences = sentenceScores.entries
            .sortedByDescending { it.value }
            .take(targetSentenceCount)
            .map { it.key }
        
        // Maintain original order
        val orderedSentences = sentences.filter { it in topSentences }
        
        // Generate final summary
        val summary = orderedSentences.joinToString(" ")
        
        // Post-process summary
        postProcessSummary(summary)
    }
    
    /**
     * Generate multiple summary types
     */
    suspend fun generateMultipleSummaries(content: String): SummaryResult = withContext(Dispatchers.IO) {
        val shortSummary = generateSummary(content, maxSentences = 1, compressionRatio = 0.1f)
        val mediumSummary = generateSummary(content, maxSentences = 3, compressionRatio = 0.3f)
        val longSummary = generateSummary(content, maxSentences = 5, compressionRatio = 0.5f)
        
        val keyPoints = extractKeyPoints(content)
        val abstractiveSummary = generateAbstractiveSummary(content)
        
        SummaryResult(
            shortSummary = shortSummary,
            mediumSummary = mediumSummary,
            longSummary = longSummary,
            keyPoints = keyPoints,
            abstractiveSummary = abstractiveSummary,
            originalLength = content.length,
            compressionRatio = mediumSummary.length.toFloat() / content.length
        )
    }
    
    /**
     * Generate summaries for multiple texts in batch
     */
    suspend fun batchGenerateSummaries(contents: List<String>): List<String> = withContext(Dispatchers.IO) {
        contents.map { content ->
            generateSummary(content)
        }
    }
    
    private fun splitIntoSentences(content: String): List<String> {
        return content.split(Regex("[.!?]+"))
            .map { it.trim() }
            .filter { it.isNotBlank() && it.length > 10 }
    }
    
    private fun calculateSentenceScores(sentences: List<String>, fullContent: String): Map<String, Float> {
        val wordFrequency = calculateWordFrequency(fullContent)
        val scores = mutableMapOf<String, Float>()
        
        sentences.forEach { sentence ->
            var score = 0f
            val words = tokenizeSentence(sentence)
            
            // TF-IDF-like scoring
            words.forEach { word ->
                val frequency = wordFrequency[word] ?: 0
                if (frequency > 0 && word !in stopWords) {
                    score += frequency.toFloat() / sentences.size
                }
            }
            
            // Position scoring (first and last sentences are often important)
            val position = sentences.indexOf(sentence)
            val positionScore = when {
                position == 0 -> 0.3f // First sentence
                position == sentences.size - 1 -> 0.2f // Last sentence
                position < sentences.size * 0.2 -> 0.1f // Early sentences
                else -> 0f
            }
            score += positionScore
            
            // Length scoring (prefer medium-length sentences)
            val lengthScore = when {
                words.size in 10..25 -> 0.2f
                words.size in 5..35 -> 0.1f
                else -> 0f
            }
            score += lengthScore
            
            // Important keyword scoring
            val keywordScore = words.count { it in importantKeywords } * 0.3f
            score += keywordScore
            
            // Numerical data scoring (numbers often indicate important facts)
            val numberScore = sentence.count { it.isDigit() } * 0.01f
            score += numberScore
            
            // Proper noun scoring (names, places are often important)
            val properNounScore = words.count { it[0].isUpperCase() } * 0.05f
            score += properNounScore
            
            scores[sentence] = score
        }
        
        return scores
    }
    
    private fun calculateWordFrequency(content: String): Map<String, Int> {
        val words = content.lowercase()
            .replace(Regex("[^a-zA-Z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 2 && it !in stopWords }
        
        return words.groupingBy { it }.eachCount()
    }
    
    private fun tokenizeSentence(sentence: String): List<String> {
        return sentence.lowercase()
            .replace(Regex("[^a-zA-Z\\s]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() && it.length > 1 }
    }
    
    private fun extractKeyPoints(content: String): List<String> {
        val sentences = splitIntoSentences(content)
        val keyPoints = mutableListOf<String>()
        
        // Look for bullet points or numbered lists
        val bulletPattern = Regex("^\\s*[-*•]\\s*(.+)", RegexOption.MULTILINE)
        val numberedPattern = Regex("^\\s*\\d+\\.\\s*(.+)", RegexOption.MULTILINE)
        
        bulletPattern.findAll(content).forEach { match ->
            keyPoints.add(match.groupValues[1].trim())
        }
        
        numberedPattern.findAll(content).forEach { match ->
            keyPoints.add(match.groupValues[1].trim())
        }
        
        // If no explicit lists, extract sentences with important keywords
        if (keyPoints.isEmpty()) {
            sentences.forEach { sentence ->
                val lowerSentence = sentence.lowercase()
                val importantWordCount = importantKeywords.count { keyword ->
                    lowerSentence.contains(keyword)
                }
                
                if (importantWordCount >= 2 || 
                    lowerSentence.contains("key") || 
                    lowerSentence.contains("important") ||
                    lowerSentence.contains("main")) {
                    keyPoints.add(sentence.trim())
                }
            }
        }
        
        return keyPoints.take(5)
    }
    
    private fun generateAbstractiveSummary(content: String): String {
        // Simple abstractive summarization using template-based approach
        val sentences = splitIntoSentences(content)
        val wordFreq = calculateWordFrequency(content)
        
        // Extract most frequent important words
        val topWords = wordFreq.entries
            .filter { it.key !in stopWords && it.key.length > 3 }
            .sortedByDescending { it.value }
            .take(10)
            .map { it.key }
        
        // Find sentences containing multiple top words
        val relevantSentences = sentences.filter { sentence ->
            val sentenceWords = tokenizeSentence(sentence)
            sentenceWords.intersect(topWords.toSet()).size >= 2
        }
        
        if (relevantSentences.isEmpty()) {
            // Fallback to extractive summary (non-suspend helper)
            return generateAbstractiveFallback(content, 2)
        }
        
        // Create abstractive summary by combining key concepts
        val keyConcepts = topWords.take(5)
        val summary = when {
            content.contains("meeting", ignoreCase = true) -> 
                "This note discusses ${keyConcepts.joinToString(", ")} in the context of a meeting or discussion."
            content.contains("project", ignoreCase = true) -> 
                "This note covers project-related topics including ${keyConcepts.joinToString(", ")}."
            content.contains("task", ignoreCase = true) || content.contains("todo", ignoreCase = true) -> 
                "This note contains tasks and action items related to ${keyConcepts.joinToString(", ")}."
            else -> 
                "This note focuses on ${keyConcepts.joinToString(", ")} and related topics."
        }
        
        return summary
    }

    // Non-suspending fallback to avoid calling suspend functions from non-suspend context
    private fun generateAbstractiveFallback(content: String, maxSentences: Int): String {
        val sentences = splitIntoSentences(content)
        if (sentences.isEmpty()) return content.take(100)
        val scores = calculateSentenceScores(sentences, content)
        val top = scores.entries.sortedByDescending { it.value }.take(maxSentences).map { it.key }
        return sentences.filter { it in top }.joinToString(" ")
    }
    
    private fun postProcessSummary(summary: String): String {
        var processed = summary
        
        // Remove incomplete sentences at the end
        if (!processed.endsWith(".") && !processed.endsWith("!") && !processed.endsWith("?")) {
            val lastSentenceEnd = maxOf(
                processed.lastIndexOf("."),
                processed.lastIndexOf("!"),
                processed.lastIndexOf("?")
            )
            if (lastSentenceEnd > processed.length * 0.7) {
                processed = processed.substring(0, lastSentenceEnd + 1)
            }
        }
        
        // Ensure proper capitalization
        processed = processed.trim()
        if (processed.isNotEmpty()) {
            processed = processed[0].uppercaseChar() + processed.substring(1)
        }
        
        // Remove redundant spaces
        processed = processed.replace(Regex("\\s+"), " ")
        
        return processed
    }
    
    /**
     * Analyze summary quality
     */
    fun analyzeSummaryQuality(original: String, summary: String): SummaryQuality {
        val originalSentences = splitIntoSentences(original)
        val summarySentences = splitIntoSentences(summary)
        
        val compressionRatio = summary.length.toFloat() / original.length
        val sentenceReduction = 1f - (summarySentences.size.toFloat() / originalSentences.size)
        
        // Calculate content preservation (simple word overlap)
        val originalWords = tokenizeSentence(original).toSet()
        val summaryWords = tokenizeSentence(summary).toSet()
        val contentPreservation = summaryWords.intersect(originalWords).size.toFloat() / originalWords.size
        
        // Calculate readability (based on sentence length and complexity)
        val avgSentenceLength = summarySentences.map { it.split(" ").size }.average().toFloat()
        val readabilityScore = when {
            avgSentenceLength in 10f..20f -> 1f
            avgSentenceLength in 5f..25f -> 0.8f
            else -> 0.6f
        }
        
        // Calculate coherence (simple heuristic based on sentence flow)
        val coherenceScore = calculateCoherence(summarySentences)
        
        return SummaryQuality(
            compressionRatio = compressionRatio,
            contentPreservation = contentPreservation,
            readabilityScore = readabilityScore,
            coherenceScore = coherenceScore,
            overallQuality = (contentPreservation + readabilityScore + coherenceScore) / 3f
        )
    }
    
    private fun calculateCoherence(sentences: List<String>): Float {
        if (sentences.size < 2) return 1f
        
        var coherenceScore = 0f
        
        for (i in 0 until sentences.size - 1) {
            val sentence1Words = tokenizeSentence(sentences[i]).toSet()
            val sentence2Words = tokenizeSentence(sentences[i + 1]).toSet()
            
            val overlap = sentence1Words.intersect(sentence2Words).size
            val union = sentence1Words.union(sentence2Words).size
            
            val similarity = if (union > 0) overlap.toFloat() / union else 0f
            coherenceScore += similarity
        }
        
        return coherenceScore / (sentences.size - 1)
    }
    
    /**
     * Generate topic-specific summaries
     */
    fun generateTopicSummary(content: String, topic: String): String {
        val sentences = splitIntoSentences(content)
        val topicWords = topic.lowercase().split(" ")
        
        // Find sentences most relevant to the topic
        val relevantSentences = sentences.map { sentence ->
            val sentenceWords = tokenizeSentence(sentence)
            val relevanceScore = topicWords.count { topicWord ->
                sentenceWords.any { it.contains(topicWord) || topicWord.contains(it) }
            }.toFloat() / topicWords.size
            
            sentence to relevanceScore
        }.filter { it.second > 0.3f }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
        
        return if (relevantSentences.isNotEmpty()) {
            relevantSentences.joinToString(" ")
        } else {
            // Fallback to a quick non-suspend summary to avoid calling suspend from non-suspend
            val sentences = splitIntoSentences(content)
            sentences.take(2).joinToString(". ").ifBlank { content.take(160) }
        }
    }
    
    /**
     * Generate progressive summaries (different levels of detail)
     */
    fun generateProgressiveSummaries(content: String): ProgressiveSummaries {
        // Use quick non-suspend fallbacks to avoid calling suspend from non-suspend
        val sentences = splitIntoSentences(content)
        val headline = sentences.firstOrNull()?.take(80) ?: content.take(80)
        val brief = sentences.take(2).joinToString(". ").ifBlank { content.take(160) }
        val detailed = sentences.take(4).joinToString(". ").ifBlank { content.take(240) }
        val comprehensive = sentences.take(6).joinToString(". ").ifBlank { content.take(320) }
        
        return ProgressiveSummaries(
            headline = headline,
            brief = brief,
            detailed = detailed,
            comprehensive = comprehensive
        )
    }
}

// Data classes for summarization
data class SummaryResult(
    val shortSummary: String,
    val mediumSummary: String,
    val longSummary: String,
    val keyPoints: List<String>,
    val abstractiveSummary: String,
    val originalLength: Int,
    val compressionRatio: Float
)

data class SummaryQuality(
    val compressionRatio: Float,
    val contentPreservation: Float,
    val readabilityScore: Float,
    val coherenceScore: Float,
    val overallQuality: Float
)

data class ProgressiveSummaries(
    val headline: String,
    val brief: String,
    val detailed: String,
    val comprehensive: String
)