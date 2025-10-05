package com.ainotebuddy.app.ai

import android.content.Context
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI Contextual Service for analyzing note relationships and context
 */
@Singleton
class AIContextualService @Inject constructor(
    private val noteRepository: NoteRepository,
    private val context: Context
) {

    suspend fun analyzeContextualRelevance(note: NoteEntity): ContextualAnalysis = withContext(Dispatchers.IO) {
        try {
            val allNotes = noteRepository.getAllNotes().first()
            val relatedNotes = findRelatedNotes(note, allNotes)
            val contextualInsights = generateContextualInsights(note, allNotes)
            val relevanceScore = calculateRelevanceScore(note, allNotes)
            
            ContextualAnalysis(
                targetNote = note,
                relatedNotes = relatedNotes,
                contextualInsights = contextualInsights,
                relevanceScore = relevanceScore,
                analysisTimestamp = System.currentTimeMillis()
            )
        } catch (e: Exception) {
            // Return minimal analysis on error
            ContextualAnalysis(
                targetNote = note,
                relatedNotes = emptyList(),
                contextualInsights = emptyList(),
                relevanceScore = 0.0f,
                analysisTimestamp = System.currentTimeMillis()
            )
        }
    }

    suspend fun findSimilarNotes(note: NoteEntity, maxResults: Int = 5): List<Long> = withContext(Dispatchers.IO) {
        try {
            val allNotes = noteRepository.getAllNotes().first()
            return@withContext findRelatedNotes(note, allNotes).take(maxResults)
        } catch (e: Exception) {
            return@withContext emptyList()
        }
    }

    private fun findRelatedNotes(targetNote: NoteEntity, allNotes: List<NoteEntity>): List<Long> {
        val targetWords = extractKeywords(targetNote)
        val similarities = mutableListOf<Pair<Long, Float>>()

        allNotes.forEach { note ->
            if (note.id != targetNote.id) {
                val noteWords = extractKeywords(note)
                val similarity = calculateSimilarity(targetWords, noteWords)
                
                if (similarity > 0.2f) { // Threshold for relevance
                    similarities.add(note.id to similarity)
                }
            }
        }

        return similarities
            .sortedByDescending { it.second }
            .take(10)
            .map { it.first }
    }

    private fun extractKeywords(note: NoteEntity): Set<String> {
        val text = "${note.title} ${note.content}".lowercase()
        val words = text.split(Regex("\\W+"))
            .filter { it.length > 3 }
            .filter { !isStopWord(it) }
        
        return words.toSet()
    }

    private fun isStopWord(word: String): Boolean {
        val stopWords = setOf(
            "the", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with", "by",
            "this", "that", "these", "those", "a", "an", "is", "are", "was", "were",
            "will", "would", "could", "should", "may", "might", "can", "shall",
            "have", "has", "had", "do", "does", "did", "get", "got", "make", "made"
        )
        return word in stopWords
    }

    private fun calculateSimilarity(words1: Set<String>, words2: Set<String>): Float {
        if (words1.isEmpty() || words2.isEmpty()) return 0f
        
        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size
        
        return intersection.toFloat() / union.toFloat()
    }

    private fun generateContextualInsights(targetNote: NoteEntity, allNotes: List<NoteEntity>): List<String> {
        val insights = mutableListOf<String>()
        
        // Category analysis
        val categoryNotes = allNotes.filter { it.category == targetNote.category && it.id != targetNote.id }
        if (categoryNotes.isNotEmpty()) {
            insights.add("Found ${categoryNotes.size} other notes in the same category")
        }

        // Tag analysis
        val targetTags = targetNote.tags?.split(",")?.map { it.trim() } ?: emptyList()
        if (targetTags.isNotEmpty()) {
            val similarTaggedNotes = allNotes.count { note ->
                note.id != targetNote.id && 
                (note.tags?.split(",")?.map { it.trim() } ?: emptyList()).any { it in targetTags }
            }
            if (similarTaggedNotes > 0) {
                insights.add("$similarTaggedNotes notes share similar tags")
            }
        }

        // Temporal analysis
        val recentNotes = allNotes.filter { 
            it.id != targetNote.id &&
            kotlin.math.abs(it.createdAt - targetNote.createdAt) < (7 * 24 * 60 * 60 * 1000) // Within a week
        }
        if (recentNotes.isNotEmpty()) {
            insights.add("${recentNotes.size} notes were created around the same time")
        }

        // Content length analysis
        val avgLength = allNotes.map { it.content.length }.average()
        when {
            targetNote.content.length > avgLength * 1.5 -> {
                insights.add("This note is significantly longer than average")
            }
            targetNote.content.length < avgLength * 0.5 -> {
                insights.add("This note is shorter than average - consider expanding")
            }
        }

        return insights
    }

    private fun calculateRelevanceScore(targetNote: NoteEntity, allNotes: List<NoteEntity>): Float {
        var score = 0.5f // Base score

        // Factor in category presence
        if (!targetNote.category.isNullOrEmpty()) {
            score += 0.1f
        }

        // Factor in tags
        val tagCount = targetNote.tags?.split(",")?.size ?: 0
        score += (tagCount * 0.05f).coerceAtMost(0.2f)

        // Factor in content length
        when {
            targetNote.content.length > 500 -> score += 0.1f
            targetNote.content.length > 1000 -> score += 0.2f
        }

        // Factor in recency of updates
        val daysSinceUpdate = (System.currentTimeMillis() - targetNote.updatedAt) / (24 * 60 * 60 * 1000)
        if (daysSinceUpdate < 7) {
            score += 0.1f
        }

        return score.coerceAtMost(1.0f)
    }
}

// Data class for contextual analysis results
data class ContextualAnalysis(
    val targetNote: NoteEntity,
    val relatedNotes: List<Long>,
    val contextualInsights: List<String>,
    val relevanceScore: Float,
    val analysisTimestamp: Long
)