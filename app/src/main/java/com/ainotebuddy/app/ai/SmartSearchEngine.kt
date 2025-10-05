package com.ainotebuddy.app.ai

import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

/**
 * Smart search engine with AI-powered capabilities
 */
@Singleton
class SmartSearchEngine @Inject constructor(
    private val noteRepository: NoteRepository
) {
    
    suspend fun search(query: String): Flow<List<SearchResult>> = flow {
        try {
            // AI pre-processing disabled for stability
            val terms = emptyList<String>()
            
            // Perform basic text search
            val basicResults = performBasicSearch(query)
            
            // Perform semantic search using derived terms
            val semanticResults = performSemanticSearch(terms)
            
            // Combine and rank results
            val combinedResults = combineAndRankResults(basicResults, semanticResults, query)
            
            emit(combinedResults)
        } catch (e: Exception) {
            // Fallback to basic search
            val basicResults = performBasicSearch(query)
            emit(basicResults.map { SearchResult(it, 0.5f, SearchResultType.BASIC_MATCH) })
        }
    }
    
    suspend fun searchByTags(tags: List<String>): Flow<List<SearchResult>> = flow {
        val allNotes = noteRepository.getAllNotes().first()
        
        val matchingNotes = allNotes.filter { note ->
            val noteTags = note.tags?.split(",")?.map { it.trim() } ?: emptyList()
            tags.any { tag -> noteTags.contains(tag) }
        }
        
        val results = matchingNotes.map { note ->
            val noteTags = note.tags?.split(",")?.map { it.trim() } ?: emptyList()
            val matchingTagCount = tags.count { tag -> noteTags.contains(tag) }
            val relevance = matchingTagCount.toFloat() / tags.size
            
            SearchResult(note, relevance, SearchResultType.TAG_MATCH)
        }.sortedByDescending { it.relevance }
        
        emit(results)
    }
    
    suspend fun searchByCategory(category: String): Flow<List<SearchResult>> = flow {
        val allNotes = noteRepository.getAllNotes().first()
        
        val categoryResults = allNotes
            .filter { note -> note.category?.equals(category, ignoreCase = true) == true }
            .map { SearchResult(it, 1.0f, SearchResultType.CATEGORY_MATCH) }
        
        emit(categoryResults)
    }
    
    suspend fun searchByDateRange(startDate: Long, endDate: Long): Flow<List<SearchResult>> = flow {
        val allNotes = noteRepository.getAllNotes().first()
        
        val dateResults = allNotes
            .filter { note -> note.createdAt in startDate..endDate }
            .map { SearchResult(it, 1.0f, SearchResultType.DATE_MATCH) }
            .sortedByDescending { it.note.createdAt }
        
        emit(dateResults)
    }
    
    suspend fun searchSimilarNotes(targetNote: NoteEntity): Flow<List<SearchResult>> = flow {
        val allNotes = noteRepository.getAllNotes().first()
        
        val similarNotes = allNotes
            .filter { it.id != targetNote.id }
            .map { note ->
                val similarity = calculateSimilarity(targetNote, note)
                SearchResult(note, similarity, SearchResultType.SEMANTIC_MATCH)
            }
            .filter { it.relevance > 0.3f }
            .sortedByDescending { it.relevance }
            .take(10)
        
        emit(similarNotes)
    }
    
    suspend fun smartSearch(query: String, filters: SearchFilters): Flow<List<SearchResult>> = flow {
        try {
            var results = search(query).first()
            
            // Apply filters
            if (filters.categories.isNotEmpty()) {
                results = results.filter { result ->
                    filters.categories.contains(result.note.category)
                }
            }
            
            if (filters.tags.isNotEmpty()) {
                results = results.filter { result ->
                    val noteTags = result.note.tags?.split(",")?.map { it.trim() } ?: emptyList()
                    filters.tags.any { tag -> noteTags.contains(tag) }
                }
            }
            
            if (filters.dateRange != null) {
                results = results.filter { result ->
                    result.note.createdAt in filters.dateRange.start..filters.dateRange.end
                }
            }
            
            if (filters.minRelevance > 0) {
                results = results.filter { it.relevance >= filters.minRelevance }
            }
            
            // Apply sorting
            results = when (filters.sortBy) {
                SortOption.RELEVANCE -> results.sortedByDescending { it.relevance }
                SortOption.DATE_CREATED -> results.sortedByDescending { it.note.createdAt }
                SortOption.DATE_MODIFIED -> results.sortedByDescending { it.note.updatedAt }
                SortOption.TITLE -> results.sortedBy { it.note.title }
                SortOption.LENGTH -> results.sortedByDescending { it.note.content.length }
            }
            
            emit(results.take(filters.maxResults))
            
        } catch (e: Exception) {
            emit(emptyList())
        }
    }
    
    suspend fun suggestSearchTerms(partialQuery: String): List<String> {
        val allNotes = noteRepository.getAllNotes().first()
        val suggestions = mutableSetOf<String>()
        
        // Extract words from all notes
        val allWords = allNotes.flatMap { note ->
            (note.title + " " + note.content).split(Regex("\\W+"))
                .filter { it.length > 3 }
                .map { it.lowercase() }
        }.distinct()
        
        // Find words that start with the partial query
        val directMatches = allWords
            .filter { it.startsWith(partialQuery.lowercase()) }
            .take(5)
        
        suggestions.addAll(directMatches)
        
        // Add common search patterns
        if (partialQuery.length > 2) {
            val fuzzyMatches = allWords
                .filter { it.contains(partialQuery.lowercase()) }
                .take(3)
            suggestions.addAll(fuzzyMatches)
        }
        
        return suggestions.toList()
    }
    
    // Private helper methods
    private suspend fun performBasicSearch(query: String): List<NoteEntity> {
        return noteRepository.getSearchResults(query).first()
    }
    
    private suspend fun performSemanticSearch(enhancedTerms: List<String>): List<NoteEntity> {
        val allNotes = noteRepository.getAllNotes().first()
        val queryTerms = enhancedTerms.map { it.lowercase() }
        
        return allNotes.filter { note ->
            val noteText = (note.title + " " + note.content).lowercase()
            queryTerms.any { term -> noteText.contains(term) }
        }
    }
    
    private fun combineAndRankResults(
        basicResults: List<NoteEntity>,
        semanticResults: List<NoteEntity>,
        originalQuery: String
    ): List<SearchResult> {
        val allNotes = (basicResults + semanticResults).distinctBy { it.id }
        
        return allNotes.map { note ->
            val relevance = calculateRelevance(note, originalQuery, basicResults.contains(note))
            val resultType = when {
                basicResults.contains(note) -> SearchResultType.EXACT_MATCH
                semanticResults.contains(note) -> SearchResultType.SEMANTIC_MATCH
                else -> SearchResultType.BASIC_MATCH
            }
            
            SearchResult(note, relevance, resultType)
        }.sortedByDescending { it.relevance }
    }
    
    private val WS: Regex = "\\s+".toRegex()

    private fun calculateRelevance(note: NoteEntity, query: String, isBasicMatch: Boolean): Float {
        val queryWords = query.trim().split(WS).filter { it.isNotEmpty() }
        val noteText = (note.title + " " + note.content).lowercase()
        val noteWords = noteText.split(Regex("\\W+")).filter { it.isNotEmpty() }
        
        var score = 0f
        
        // Exact matches in title get higher score
        queryWords.forEach { queryWord ->
            if (note.title.lowercase().contains(queryWord)) {
                score += 2.0f
            }
            if (note.content.lowercase().contains(queryWord)) {
                score += 1.0f
            }
        }
        
        // Bonus for basic match
        if (isBasicMatch) {
            score += 1.0f
        }
        
        // Normalize by note length
        val normalizedScore = score / (noteWords.size.toFloat() / 100f).coerceAtLeast(1f)
        
        return min(normalizedScore / 10f, 1.0f) // Normalize to 0-1 range
    }
    
    private fun calculateSimilarity(note1: NoteEntity, note2: NoteEntity): Float {
        val text1Words = (note1.title + " " + note1.content)
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 3 }
            .toSet()
        
        val text2Words = (note2.title + " " + note2.content)
            .lowercase()
            .split(Regex("\\W+"))
            .filter { it.length > 3 }
            .toSet()
        
        val intersection = text1Words.intersect(text2Words).size
        val union = text1Words.union(text2Words).size
        
        return if (union == 0) 0f else intersection.toFloat() / union.toFloat()
    }
}

// Data classes
data class SearchResult(
    val note: NoteEntity,
    val relevance: Float,
    val resultType: SearchResultType,
    val highlights: List<TextHighlight> = emptyList()
)

data class TextHighlight(
    val start: Int,
    val end: Int,
    val text: String
)

data class SearchFilters(
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val dateRange: DateRange? = null,
    val minRelevance: Float = 0f,
    val maxResults: Int = 50,
    val sortBy: SortOption = SortOption.RELEVANCE
)

data class DateRange(
    val start: Long,
    val end: Long
)

enum class SearchResultType {
    EXACT_MATCH,
    SEMANTIC_MATCH,
    TAG_MATCH,
    CATEGORY_MATCH,
    DATE_MATCH,
    BASIC_MATCH
}

enum class SortOption {
    RELEVANCE,
    DATE_CREATED,
    DATE_MODIFIED,
    TITLE,
    LENGTH
}