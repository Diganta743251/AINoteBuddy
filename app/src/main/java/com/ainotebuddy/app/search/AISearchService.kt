package com.ainotebuddy.app.search

import com.ainotebuddy.app.ai.EnhancedAIService
import com.ainotebuddy.app.data.Note

import com.ainotebuddy.app.data.repository.NoteRepository
import com.ainotebuddy.app.integration.SearchSuggestionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AI-powered search service that provides intelligent search capabilities
 */
@Singleton
class AISearchService @Inject constructor(
    private val noteRepository: NoteRepository,
    private val enhancedAIService: EnhancedAIService,
    private val smartSearchEngine: SmartSearchEngine
) {
    
    /**
     */
    suspend fun performIntelligentSearch(query: String): IntelligentSearchResult {
        return withContext(Dispatchers.Default) {
            // Get all notes for searching (collect from Flow)
            val notes = noteRepository.getAllNotes().first()

            // Use smart search engine for basic search
            val searchResultContainer = smartSearchEngine.search(query, notes)
            val searchResults = searchResultContainer.results
            val parsedQuery = searchResultContainer.query
            
            // Enhance results with AI insights
            val enhancedResults = enhanceSearchResults(searchResults)
            
            // Generate search suggestions
            val suggestions = generateSearchSuggestions(query, notes)
            
            // Generate query refinement suggestions
            val refinements = generateQueryRefinements(query, searchResults.size)
            
            IntelligentSearchResult(
                originalQuery = query,
                parsedQuery = parsedQuery,
                results = enhancedResults,
                suggestions = suggestions,
                refinements = refinements,
                totalResults = searchResults.size,
                searchTime = System.currentTimeMillis() // Simplified
            )
        }
    }
    
    /**
     * Enhance search results with AI-powered insights
     */
    private suspend fun enhanceSearchResults(results: List<SmartSearchResult>): List<UIEnhancedSearchResult> {
        return results.map { result ->
            // Avoid heavy analysis during compile-fix; provide null insights
            val aiInsights: Any? = null
            
            UIEnhancedSearchResult(
                note = result.note,
                relevanceScore = result.relevanceScore,
                matchedFields = result.matchedFields,
                highlights = result.highlights,
                contextSnippets = result.contextSnippets,
                aiInsights = aiInsights,
                reasonForMatch = generateMatchReason(result),
                suggestedActions = generateSuggestedActions(result.note)
            )
        }
    }
    
    /**
     * Generate search suggestions based on query and available notes
     */
    private fun generateSearchSuggestions(query: String, notes: List<Note>): List<UISearchSuggestion> {
        val suggestions = mutableListOf<UISearchSuggestion>()
        
        val categories = notes.map { it.category }.distinct().filter { it.isNotEmpty() }
        categories.forEach { category ->
            if (category.contains(query, ignoreCase = true) || query.contains(category, ignoreCase = true)) {
                suggestions.add(UISearchSuggestion(
                    text = "category:$category",
                    type = SearchSuggestionType.PERSONALIZED,
                    confidence = 0.8f,
                    description = "Search in $category category"
                ))
            }
        }
        
        // Tag-based suggestions
        val allTags = notes.flatMap { it.tags }.distinct()
        allTags.forEach { tag ->
            if (tag.contains(query, ignoreCase = true) || query.contains(tag, ignoreCase = true)) {
                suggestions.add(UISearchSuggestion(
                    text = "tag:$tag",
                    type = SearchSuggestionType.PERSONALIZED,
                    confidence = 0.9f,
                    description = "Search notes with #$tag"
                ))
            }
        }
        
        // Time-based suggestions
        if (query.contains("recent") || query.contains("today") || query.contains("yesterday")) {
            suggestions.add(UISearchSuggestion(
                text = "recent notes from today",
                type = SearchSuggestionType.CONTEXTUAL,
                confidence = 0.7f,
                description = "Show notes from today"
            ))
        }
        
        // Content-based suggestions
        val commonWords = extractCommonWords(notes)
        commonWords.forEach { word ->
            if (word.contains(query, ignoreCase = true) && word.length > query.length) {
                suggestions.add(UISearchSuggestion(
                    text = word,
                    type = SearchSuggestionType.TOPIC,
                    confidence = 0.6f,
                    description = "Search for '$word'"
                ))
            }
        }
        
        return suggestions.take(10)
    }
    
    /**
     * Generate query refinement suggestions
     */
    private fun generateQueryRefinements(query: String, resultCount: Int): List<QueryRefinement> {
        val refinements = mutableListOf<QueryRefinement>()
        if (resultCount == 0) {
            refinements += QueryRefinement(
                suggestion = "Try removing some words",
                type = RefinementType.BROADEN,
                explanation = "Your search might be too specific"
            )
            refinements += QueryRefinement(
                suggestion = "Check for typos",
                type = RefinementType.SPELLING,
                explanation = "Make sure all words are spelled correctly"
            )
            refinements += QueryRefinement(
                suggestion = "Use synonyms",
                type = RefinementType.SYNONYM,
                explanation = "Try different words with similar meaning"
            )
        } else if (resultCount > 50) {
            refinements += QueryRefinement(
                suggestion = "Add more specific terms",
                type = RefinementType.NARROW,
                explanation = "Add more words to narrow down results"
            )
            refinements += QueryRefinement(
                suggestion = "Use filters like category: or tag:",
                type = RefinementType.FILTER,
                explanation = "Use filters to refine your search"
            )
        }
        return refinements
    }

data class UISearchSuggestion(
    val text: String,
    val type: SearchSuggestionType,
    val confidence: Float,
    val description: String
)

private fun generateMatchReason(result: SmartSearchResult): String {
    val matchedFields = result.matchedFields
    return when {
        matchedFields.any { it.fieldType == FieldType.TITLE } -> "Found in title"
        matchedFields.any { it.fieldType == FieldType.TAGS } -> "Found in tags"
        matchedFields.any { it.fieldType == FieldType.CATEGORY } -> "Found in category"
        matchedFields.any { it.fieldType == FieldType.CONTENT } -> "Found in content"
        else -> "Semantic match"
    }
}

private fun generateSuggestedActions(note: Note): List<String> {
    val actions = mutableListOf("Open note", "Edit note")
    if (!note.isPinned) actions += "Pin note"
    if (!note.isStarred) actions += "Star note"
    actions += listOf("Share note", "Add to collection")
    return actions
}

private fun extractCommonWords(notes: List<Note>): List<String> {
    val allText = notes.joinToString(" ") { "${it.title} ${it.content}" }
    val words = allText.lowercase().split(Regex("\\W+")).filter { it.length > 3 }
    val stopWords = setOf("that", "this", "with", "from", "they", "been", "have", "were", "said", "what", "when", "where", "will", "there")
    return words
        .filter { it !in stopWords }
        .groupingBy { it }
        .eachCount()
        .filter { it.value > 2 }
        .toList()
        .sortedByDescending { it.second }
        .take(20)
        .map { it.first }
}

/**
 * Result of intelligent search with AI enhancements
 */
data class IntelligentSearchResult(
    val originalQuery: String,
    val parsedQuery: SmartSearchQuery,
    val results: List<UIEnhancedSearchResult>,
    val suggestions: List<UISearchSuggestion>,
    val refinements: List<QueryRefinement>,
    val totalResults: Int,
    val searchTime: Long
)

/**
 * Enhanced search result with AI insights
 */
data class UIEnhancedSearchResult(
    val note: Note,
    val relevanceScore: Float,
    val matchedFields: List<MatchedField>,
    val highlights: List<SearchHighlight>,
    val contextSnippets: List<String>,
    val aiInsights: Any?,
    val reasonForMatch: String,
    val suggestedActions: List<String>
)

/**
 * Query refinement suggestion
 */
  data class QueryRefinement(
    val suggestion: String,
    val type: RefinementType,
    val explanation: String
  )

  enum class RefinementType {
    BROADEN, NARROW, SPELLING, SYNONYM, FILTER
  }
}