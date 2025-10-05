package com.ainotebuddy.app.search

import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*

data class BasicSearchQuery(
    val text: String = "",
    val tags: List<String> = emptyList(),
    val dateRange: BasicDateRange? = null,
    val isPinned: Boolean? = null,
    val isFavorite: Boolean? = null,
    val sortBy: BasicSortOption = BasicSortOption.RELEVANCE
)

data class BasicDateRange(
    val start: Long,
    val end: Long
)

enum class BasicSortOption {
    RELEVANCE,
    DATE_CREATED,
    DATE_MODIFIED,
    TITLE,
    LENGTH
}

data class BasicSearchResult(
    val note: NoteEntity,
    val relevanceScore: Float = 0f,
    val matchedFields: List<String> = emptyList(),
    val snippet: String = ""
)

@Singleton
class EnhancedSearchEngine @Inject constructor(
    private val noteRepository: NoteRepository
) {
    
    suspend fun search(query: BasicSearchQuery): Flow<List<BasicSearchResult>> {
        return noteRepository.getAllNotes().map { notes ->
            val results = notes
                .filter { note -> matchesQuery(note, query) }
                .map { note -> createSearchResult(note, query) }

            val sorted = when (query.sortBy) {
                BasicSortOption.RELEVANCE -> results.sortedByDescending { it.relevanceScore }
                BasicSortOption.DATE_CREATED -> results.sortedByDescending { it.note.createdAt }
                BasicSortOption.DATE_MODIFIED -> results.sortedByDescending { it.note.updatedAt }
                BasicSortOption.TITLE -> results.sortedBy { it.note.title.lowercase() }
                BasicSortOption.LENGTH -> results.sortedByDescending { it.note.content.length }
            }
            sorted
        }
    }
    
    private fun matchesQuery(note: NoteEntity, query: BasicSearchQuery): Boolean {
        
        // Text matching
        if (query.text.isNotBlank()) {
            val searchText = query.text.lowercase()
            val titleMatch = note.title.lowercase().contains(searchText)
            val contentMatch = note.content.lowercase().contains(searchText)
            val tagMatch = note.tags.split(',').map { it.trim().lowercase() }.any { it.contains(searchText) }
            
            if (!titleMatch && !contentMatch && !tagMatch) {
                return false
            }
        }
        
        // Tag filtering (NoteEntity.tags is comma-separated)
        if (query.tags.isNotEmpty()) {
            val noteTagsLower = note.tags.split(',').map { it.trim().lowercase() }.filter { it.isNotEmpty() }
            val queryTagsLower = query.tags.map { it.lowercase() }
            if (!noteTagsLower.containsAll(queryTagsLower)) {
                return false
            }
        }
        
        // Date range filtering
        query.dateRange?.let { range ->
            if (note.createdAt < range.start || note.createdAt > range.end) {
                return false
            }
        }
        
        // Pin status filtering
        query.isPinned?.let { pinned ->
            if (note.isPinned != pinned) {
                return false
            }
        }
        
        // Favorite status filtering
        query.isFavorite?.let { favorite ->
            if (note.isFavorite != favorite) {
                return false
            }
        }
        
        return true
    }
    
    private fun createSearchResult(note: NoteEntity, query: BasicSearchQuery): BasicSearchResult {
        val matchedFields = mutableListOf<String>()
        val searchText = query.text.lowercase()
        var relevanceScore = 0f
        
        // Calculate relevance score and matched fields
        if (query.text.isNotBlank()) {
            // Title match (highest weight)
            if (note.title.lowercase().contains(searchText)) {
                matchedFields.add("title")
                relevanceScore += 3f
                
                // Exact title match bonus
                if (note.title.lowercase() == searchText) {
                    relevanceScore += 2f
                }
            }
            
            // Content match
            if (note.content.lowercase().contains(searchText)) {
                matchedFields.add("content")
                relevanceScore += 1f
                // Multiple occurrences bonus
                val occurrences = note.content.lowercase().split(searchText).size - 1
                relevanceScore += (occurrences - 1) * 0.5f
            }
            
            // Tag match (NoteEntity.tags is comma-separated)
            note.tags.split(',').map { it.trim().lowercase() }.forEach { tag ->
                if (tag.contains(searchText)) {
                    matchedFields.add("tags")
                    relevanceScore += 2f
                }
            }
        }
        
        // Recency bonus
        val daysSinceModified = (System.currentTimeMillis() - note.updatedAt) / (24 * 60 * 60 * 1000)
        relevanceScore += Math.max(0f, (30f - daysSinceModified) / 30f)
        
        // Pin and favorite bonus
        if (note.isPinned) relevanceScore += 0.5f
        if (note.isFavorite) relevanceScore += 0.3f
        
        val snippet = generateSnippet(note, query.text)
        
        return BasicSearchResult(
            note = note,
            relevanceScore = relevanceScore,
            matchedFields = matchedFields,
            snippet = snippet
        )
    }
    
    private fun generateSnippet(note: NoteEntity, searchText: String): String {
        if (searchText.isBlank()) {
            return note.content.take(150) + if (note.content.length > 150) "..." else ""
        }
        
        val searchLower = searchText.lowercase()
        val content = note.content
        val contentLower = content.lowercase()
        
        val index = contentLower.indexOf(searchLower)
        if (index == -1) {
            return note.content.take(150) + if (note.content.length > 150) "..." else ""
        }
        
        val start = Math.max(0, index - 75)
        val end = Math.min(content.length, index + searchText.length + 75)
        
        val snippet = content.substring(start, end)
        val prefix = if (start > 0) "..." else ""
        val suffix = if (end < content.length) "..." else ""
        
        return prefix + snippet + suffix
    }

    private fun getSortKey(result: BasicSearchResult, sortBy: BasicSortOption): Comparable<*> {
        return when (sortBy) {
            BasicSortOption.RELEVANCE -> -result.relevanceScore // Negative for descending
            BasicSortOption.DATE_CREATED -> -result.note.createdAt // Negative for descending
            BasicSortOption.DATE_MODIFIED -> -result.note.updatedAt // Negative for descending
            BasicSortOption.TITLE -> result.note.title.lowercase()
            BasicSortOption.LENGTH -> -result.note.content.length // Negative for descending
        }
    }
    fun getSuggestedTags(): List<String> {
        // In a real implementation, this would analyze existing notes
        return listOf("work", "personal", "ideas", "important", "draft", "completed")
    }
    
    fun getPopularSearches(): List<String> {
        // In a real implementation, this would track usage analytics
        return listOf("today", "urgent", "review", "meeting", "project")
    }
}