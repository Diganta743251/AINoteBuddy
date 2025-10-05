package com.ainotebuddy.app.search

import android.content.Context
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.utils.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import kotlin.math.max

/**
 * Processes natural language search queries and converts them into structured search parameters.
 */
class NLPSearchProcessor(private val context: Context) {

    /**
     * Processes a natural language query and returns structured search parameters.
     */
    suspend fun processQuery(query: String): SearchParameters = withContext(Dispatchers.Default) {
        val normalizedQuery = query.lowercase(Locale.getDefault())
        
        SearchParameters(
            query = extractMainQuery(normalizedQuery),
            dateRange = extractDateRange(normalizedQuery),
            tags = extractTags(normalizedQuery),
            priority = extractPriority(normalizedQuery),
            isPinned = extractPinnedStatus(normalizedQuery)
        )
    }
    
    private fun extractMainQuery(query: String): String {
        // Remove common date and filter terms to get the main search query
        val filterTerms = listOf(
            "today", "yesterday", "this week", "last week", "this month", 
            "last month", "pinned", "high priority", "medium priority", "low priority"
        )
        
        return query.split(" ").filterNot { word ->
            filterTerms.any { it.contains(word) }
        }.joinToString(" ")
    }
    
    private fun extractDateRange(query: String): Pair<Long, Long>? {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        return when {
            "today" in query -> {
                val start = DateUtils.startOfDay(now)
                val end = DateUtils.endOfDay(now)
                start to end
            }
            "yesterday" in query -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                val start = DateUtils.startOfDay(calendar.timeInMillis)
                val end = DateUtils.endOfDay(calendar.timeInMillis)
                start to end
            }
            "this week" in query -> {
                val start = DateUtils.startOfWeek(now)
                val end = DateUtils.endOfDay(now)
                start to end
            }
            "last week" in query -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                val start = DateUtils.startOfWeek(calendar.timeInMillis)
                val end = DateUtils.endOfWeek(calendar.timeInMillis)
                start to end
            }
            "this month" in query -> {
                val start = DateUtils.startOfMonth(now)
                val end = DateUtils.endOfDay(now)
                start to end
            }
            "last month" in query -> {
                calendar.timeInMillis = now
                calendar.add(Calendar.MONTH, -1)
                val start = DateUtils.startOfMonth(calendar.timeInMillis)
                val end = DateUtils.endOfMonth(calendar.timeInMillis)
                start to end
            }
            else -> null
        }
    }
    
    private fun extractTags(query: String): List<String> {
        val tags = mutableListOf<String>()
        val words = query.split(" ")
        
        words.forEachIndexed { index, word ->
            if (word.startsWith("#") && word.length > 1) {
                tags.add(word.substring(1))
            }
        }
        
        return tags
    }
    
    private fun extractPriority(query: String): Int? {
        return when {
            "high priority" in query -> 2
            "medium priority" in query -> 1
            "low priority" in query -> 0
            else -> null
        }
    }
    
    private fun extractPinnedStatus(query: String): Boolean? {
        return when {
            "pinned" in query -> true
            else -> null
        }
    }
    
    /**
     * Generates query suggestions based on the current input.
     */
    suspend fun getSuggestions(query: String, notes: List<Note>): List<String> = withContext(Dispatchers.Default) {
        if (query.length < 2) return@withContext emptyList()
        
        val suggestions = mutableListOf<String>()
        val normalizedQuery = query.lowercase(Locale.getDefault())
        
        // Add date-based suggestions
        suggestions.addAll(getDateBasedSuggestions(normalizedQuery))
        
        // Add tag-based suggestions
        suggestions.addAll(getTagSuggestions(normalizedQuery, notes))
        
        // Add priority-based suggestions
        if ("prio" in normalizedQuery) {
            suggestions.add("high priority")
            suggestions.add("medium priority")
            suggestions.add("low priority")
        }
        
        // Add pinned suggestion
        if ("pin" in normalizedQuery) {
            suggestions.add("pinned")
        }
        
        suggestions.distinct().take(5)
    }
    
    private fun getDateBasedSuggestions(query: String): List<String> {
        val suggestions = mutableListOf<String>()
        
        if ("tod" in query) suggestions.add("today")
        if ("yest" in query) suggestions.add("yesterday")
        if ("week" in query) {
            suggestions.add("this week")
            suggestions.add("last week")
        }
        if ("month" in query) {
            suggestions.add("this month")
            suggestions.add("last month")
        }
        
        return suggestions
    }
    
    private fun getTagSuggestions(query: String, notes: List<Note>): List<String> {
        if (!query.contains('#')) return emptyList()
        
        val allTags = notes.flatMap { it.tags ?: emptyList() }.distinct()
        val currentTagPrefix = query.substringAfterLast('#', "")
        
        return allTags
            .filter { it.startsWith(currentTagPrefix, ignoreCase = true) }
            .take(3)
            .map { "${query.takeWhile { c -> c != '#' }}#$it" }
    }
}

/**
 * Represents structured search parameters extracted from a natural language query.
 */
data class SearchParameters(
    val query: String,
    val dateRange: Pair<Long, Long>? = null,
    val tags: List<String> = emptyList(),
    val priority: Int? = null,
    val isPinned: Boolean? = null
)
