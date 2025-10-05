package com.ainotebuddy.app.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ainotebuddy.app.data.Note
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.*

// DataStore extension
private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore(name = "smart_search")

/**
 * Advanced search query with natural language processing capabilities
 */
@Serializable
data class SmartSearchQuery(
    val rawQuery: String,
    val processedQuery: String = "",
    val searchTerms: List<String> = emptyList(),
    val filters: SearchFilters = SearchFilters(),
    val searchType: SearchType = SearchType.GENERAL,
    val semanticIntent: SemanticIntent = SemanticIntent.UNKNOWN
)

@Serializable
data class SearchFilters(
    val dateRange: DateRange? = null,
    val categories: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val noteTypes: List<NoteType> = emptyList(),
    val hasAttachments: Boolean? = null,
    val isPinned: Boolean? = null,
    val priority: Int? = null,
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val createdBy: String? = null,
    val lastModifiedRange: DateRange? = null
)

@Serializable
data class DateRange(
    val startDate: Long,
    val endDate: Long,
    val relativeType: RelativeDateType? = null
)

enum class RelativeDateType {
    TODAY, YESTERDAY, THIS_WEEK, LAST_WEEK, THIS_MONTH, LAST_MONTH, THIS_YEAR
}

/**
 * Extract last-modified date range from natural language; basic keywords only.
 */
private fun extractLastModifiedRange(query: String): DateRange? {
    val lower = query.lowercase()
    return when {
        lower.contains("modified today") || lower.contains("updated today") -> {
            extractRelativeDateRange("today")
        }
        lower.contains("modified yesterday") || lower.contains("updated yesterday") -> {
            extractRelativeDateRange("yesterday")
        }
        lower.contains("modified this week") || lower.contains("updated this week") -> {
            extractRelativeDateRange("this week")
        }
        lower.contains("modified last week") || lower.contains("updated last week") -> {
            extractRelativeDateRange("last week")
        }
        lower.contains("modified this month") || lower.contains("updated this month") -> {
            extractRelativeDateRange("this month")
        }
        else -> null
    }
}

// Standalone helper for relative date ranges used by extractLastModifiedRange
private fun extractRelativeDateRange(keyword: String): DateRange? {
    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()
    return when (keyword.lowercase()) {
        "today" -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            val startOfDay = calendar.timeInMillis
            DateRange(startOfDay, now, RelativeDateType.TODAY)
        }
        "yesterday" -> {
            calendar.add(Calendar.DAY_OF_YEAR, -1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfYesterday = calendar.timeInMillis
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            val endOfYesterday = calendar.timeInMillis
            DateRange(startOfYesterday, endOfYesterday, RelativeDateType.YESTERDAY)
        }
        "this week" -> {
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfWeek = calendar.timeInMillis
            DateRange(startOfWeek, now, RelativeDateType.THIS_WEEK)
        }
        "last week" -> {
            calendar.add(Calendar.WEEK_OF_YEAR, -1)
            calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfLastWeek = calendar.timeInMillis
            calendar.add(Calendar.DAY_OF_YEAR, 6)
            calendar.set(Calendar.HOUR_OF_DAY, 23)
            calendar.set(Calendar.MINUTE, 59)
            val endOfLastWeek = calendar.timeInMillis
            DateRange(startOfLastWeek, endOfLastWeek, RelativeDateType.LAST_WEEK)
        }
        "this month" -> {
            calendar.set(Calendar.DAY_OF_MONTH, 1)
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            val startOfMonth = calendar.timeInMillis
            DateRange(startOfMonth, now, RelativeDateType.THIS_MONTH)
        }
        else -> null
    }
}

/**
 * Parse attachment filter from query text
 */
private fun extractAttachmentFilter(lowerQuery: String): Boolean? {
    val q = lowerQuery.lowercase()
    return when {
        q.contains("with attachment") || q.contains("has attachments") || q.contains("with attachments") -> true
        q.contains("without attachments") || q.contains("no attachments") -> false
        else -> null
    }
}

/**
 * Parse pinned filter from query text
 */
private fun extractPinnedFilter(lowerQuery: String): Boolean? {
    val q = lowerQuery.lowercase()
    return when {
        q.contains("pinned") -> true
        q.contains("not pinned") || q.contains("unpinned") -> false
        else -> null
    }
}

/**
 * Parse simple length filters like "long" or "short"; default no constraints
 */
private fun extractLengthFilters(lowerQuery: String): Pair<Int?, Int?> {
    val q = lowerQuery.lowercase()
    return when {
        q.contains("short notes") -> Pair(null, 200)
        q.contains("long notes") -> Pair(500, null)
        else -> Pair(null, null)
    }
}

enum class SearchType {
    GENERAL, SEMANTIC, EXACT_PHRASE, FUZZY, REGEX, ADVANCED
}

enum class SemanticIntent {
    UNKNOWN, FIND_RECENT, FIND_BY_TOPIC, FIND_BY_DATE, FIND_BY_TYPE, 
    FIND_RELATED, FIND_IMPORTANT, FIND_UNFINISHED, FIND_BY_LOCATION
}

enum class NoteType {
    TEXT, VOICE, IMAGE, DRAWING, DOCUMENT, CHECKLIST, MEETING, JOURNAL
}

/**
 * Search result with relevance scoring and highlighting
 */
data class SmartSearchResult(
    val note: Note,
    val relevanceScore: Float,
    val matchedFields: List<MatchedField>,
    val highlights: List<SearchHighlight>,
    val semanticSimilarity: Float = 0f,
    val contextSnippets: List<String> = emptyList()
)

@Serializable
data class MatchedField(
    val fieldName: String,
    val fieldType: FieldType,
    val matchStrength: Float,
    val exactMatch: Boolean = false
)

enum class FieldType {
    TITLE, CONTENT, TAGS, CATEGORY, METADATA, ATTACHMENT_NAME
}

@Serializable
data class SearchHighlight(
    val text: String,
    val startIndex: Int,
    val endIndex: Int,
    val fieldType: FieldType,
    val matchType: HighlightType
)

enum class HighlightType {
    EXACT_MATCH, PARTIAL_MATCH, SEMANTIC_MATCH, FUZZY_MATCH
}

/**
 * Search suggestions and auto-completion
 */
@Serializable
data class SearchSuggestion(
    val suggestion: String,
    val type: SuggestionType,
    val confidence: Float,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L
)

enum class SuggestionType {
    QUERY_COMPLETION, QUERY_REFINEMENT, SAVED_SEARCH, RECENT_SEARCH, 
    POPULAR_SEARCH, SEMANTIC_EXPANSION, FILTER_SUGGESTION
}

/**
 * Saved search presets that integrate with dashboard presets
 */
@Serializable
data class SavedSearchPreset(
    val id: String,
    val name: String,
    val description: String,
    val query: SmartSearchQuery,
    val category: SearchPresetCategory,
    val isDefault: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L,
    val createdAt: Long = System.currentTimeMillis()
)

enum class SearchPresetCategory {
    WORK, PERSONAL, ACADEMIC, CREATIVE, PRODUCTIVITY, RECENT, FAVORITES
}

/**
 * Main Smart Search Engine with AI-powered capabilities
 */
@Singleton
class SmartSearchEngine @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val json = Json { ignoreUnknownKeys = true }
    
    // Search analytics and history
    private val searchHistoryKey = stringPreferencesKey("search_history")
    private val searchAnalyticsKey = stringPreferencesKey("search_analytics")
    private val savedSearchesKey = stringPreferencesKey("saved_searches")
    
    // Search index and caching
    private val searchIndex = mutableMapOf<String, LocalSearchIndexEntry>()
    private val queryCache = mutableMapOf<String, List<SmartSearchResult>>()
    private val maxCacheSize = 100
    
    // Visual search engine
    private val visualSearchEngine by lazy { VisualSearchEngine(context) }
    
    /**
     * Main search function with natural language processing
     */
    fun search(
        query: String,
        notes: List<Note> = emptyList(),
        maxResults: Int = 50
    ): SearchResults {
        val searchQuery = parseNaturalLanguageQuery(query)
        return searchWithParams(
            query = searchQuery.processedQuery,
            dateRange = searchQuery.filters.dateRange,
            tags = searchQuery.filters.tags,
            priority = searchQuery.filters.priority,
            isPinned = searchQuery.filters.isPinned,
            notes = notes,
            maxResults = maxResults
        )
    }
    
    /**
     * Search with structured parameters from NLP processing
     */
    fun searchWithParams(
        query: String,
        dateRange: DateRange? = null,
        tags: List<String> = emptyList(),
        priority: Int? = null,
        isPinned: Boolean? = null,
        notes: List<Note> = emptyList(),
        maxResults: Int = 50
    ): SearchResults {
        val searchQuery = SmartSearchQuery(
            rawQuery = query,
            processedQuery = query,
            filters = SearchFilters(
                dateRange = dateRange,
                tags = tags,
                priority = priority,
                isPinned = isPinned
            )
        )
        val startTime = System.currentTimeMillis()
        
        // Parse and process the natural language query
        val smartQuery = parseNaturalLanguageQuery(query)
        
        // Record search analytics
        recordSearchQuery(smartQuery)
        
        // Check cache first
        val cacheKey = "${smartQuery.processedQuery}_${smartQuery.filters.hashCode()}"
        queryCache[cacheKey]?.let { cachedResults ->
            return SearchResults(
                query = smartQuery,
                results = cachedResults.take(maxResults),
                totalResults = cachedResults.size,
                searchTime = System.currentTimeMillis() - startTime,
                fromCache = true
            )
        }
        
        // Perform the search
        val results = when (smartQuery.searchType) {
            SearchType.SEMANTIC -> performSemanticSearch(smartQuery, notes)
            SearchType.EXACT_PHRASE -> performExactPhraseSearch(smartQuery, notes)
            SearchType.FUZZY -> performFuzzySearch(smartQuery, notes)
            SearchType.REGEX -> performRegexSearch(smartQuery, notes)
            SearchType.ADVANCED -> performAdvancedSearch(smartQuery, notes)
            else -> performGeneralSearch(smartQuery, notes)
        }
        
        // Sort by relevance and apply filters
        val filteredResults = applyFilters(results, smartQuery.filters)
            .sortedByDescending { it.relevanceScore }
            .take(maxResults)
        
        // Cache results
        if (queryCache.size >= maxCacheSize) {
            queryCache.remove(queryCache.keys.first())
        }
        queryCache[cacheKey] = filteredResults
        
        return SearchResults(
            query = smartQuery,
            results = filteredResults,
            totalResults = results.size,
            searchTime = System.currentTimeMillis() - startTime,
            fromCache = false,
            suggestions = generateSearchSuggestions(smartQuery, filteredResults)
        )
    }
    
    /**
     * Parse natural language query into structured search query with enhanced NLP capabilities
     */
    private fun parseNaturalLanguageQuery(query: String): SmartSearchQuery {
        val normalizedQuery = query.trim()
        val lowerQuery = normalizedQuery.lowercase()
        
        // Detect search intent with enhanced pattern matching
        val semanticIntent = detectSemanticIntent(normalizedQuery, lowerQuery)
        
        // Extract date ranges with improved natural language support
        val dateRange = extractDateRange(normalizedQuery)
        val lastModifiedRange = extractLastModifiedRange(normalizedQuery)
        
        // Extract advanced filters with better pattern matching
        val categories = extractCategories(normalizedQuery)
        val tags = extractTags(normalizedQuery)
        val noteTypes = extractNoteTypes(normalizedQuery)
        val hasAttachments = extractAttachmentFilter(lowerQuery)
        val isPinned = extractPinnedFilter(lowerQuery)
        val lengthFilters = extractLengthFilters(lowerQuery)
        
        // Determine search type based on query characteristics
        val searchType = determineSearchType(normalizedQuery)
        
        // Extract and clean search terms with better filtering of filter keywords
        val searchTerms = extractSearchTerms(normalizedQuery, categories, tags, noteTypes)
        
        // Build processed query with semantic enhancements
        val processedQuery = buildProcessedQuery(searchTerms, semanticIntent)
        
        return SmartSearchQuery(
            rawQuery = query,
            processedQuery = processedQuery,
            searchTerms = searchTerms,
            filters = SearchFilters(
                dateRange = dateRange,
                categories = categories,
                tags = tags,
                noteTypes = noteTypes,
                hasAttachments = hasAttachments,
                isPinned = isPinned,
                minLength = lengthFilters.first,
                maxLength = lengthFilters.second,
                lastModifiedRange = lastModifiedRange
            ),
            searchType = searchType,
            semanticIntent = semanticIntent
        )
    }
    
    /**
     * Enhanced semantic intent detection with more sophisticated pattern matching
     */
    private fun detectSemanticIntent(query: String, lowerQuery: String): SemanticIntent {
        // Check for date-based intents first
        val datePatterns = listOf(
            "(?:find|show|search).*?(?:from|since|between|before|after|on|last|next|this|tomorrow|yesterday|today|week|month|year|jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)",
            "(?:recent|latest|new) (?:notes|items|entries)",
            "(?:created|modified|updated) (?:before|after|on|between)"
        )
        
        if (datePatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
            return SemanticIntent.FIND_BY_DATE
        }
        
        // Check for content-based intents
        val contentPatterns = listOf(
            "(?:about|regarding|related to|concerning|containing|with|that contains|including)",
            "(?:find|search|show) (?:me )?(?:all )?(?:the )?(?:notes|items|entries) (?:about|related to|containing)"
        )
        
        if (contentPatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
            return SemanticIntent.FIND_BY_TOPIC
        }
        
        // Check for specific note types
        val typePatterns = listOf(
            "(?:type|kind|format|that is|which is) (?:a )?(?:note|voice memo|image|drawing|document|checklist|meeting notes|journal)",
            "(?:voice|audio|image|photo|picture|drawing|sketch|document|pdf|checklist|todo|meeting|journal) (?:note|entry|item)"
        )
        
        if (typePatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
            return SemanticIntent.FIND_BY_TYPE
        }
        
        // Check for related content
        val relatedPatterns = listOf(
            "(?:similar|like|related|connected|relevant) (?:to|with|for)",
            "(?:find|show|search) (?:me )?(?:similar|related|relevant) (?:notes|items|entries)"
        )
        
        if (relatedPatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
            return SemanticIntent.FIND_RELATED
        }
        
        // Check for important/priority content
        val importantPatterns = listOf(
            "(?:important|pinned|starred|favorite|priority|flagged)",
            "(?:find|show|search) (?:me )?(?:important|pinned|starred|favorite|priority|flagged)"
        )
        
        if (importantPatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
            return SemanticIntent.FIND_IMPORTANT
        }
        
        // Check for incomplete/todo items
        val todoPatterns = listOf(
            "(?:todo|to do|to-do|unfinished|incomplete|pending|open|not done|not completed)",
            "(?:find|show|search) (?:me )?(?:unfinished|incomplete|pending|open|not done|not completed)"
        )
        
        if (todoPatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
            return SemanticIntent.FIND_UNFINISHED
        }
        
        // Check for location-based content
        val locationPatterns = listOf(
            "(?:at|in|near|close to|around) .*?(?:location|place|address|area|city|country|gps|coordinates)",
            "(?:find|show|search) (?:me )?(?:notes|items|entries) (?:from|in|near|around)"
        )
        
        if (locationPatterns.any { Regex(it, RegexOption.IGNORE_CASE).containsMatchIn(query) }) {
            return SemanticIntent.FIND_BY_LOCATION
        }
        
        // Check for recent content if no other intent is detected and query is short
        if (query.split("\\s+".toRegex()).size <= 4) {
            val recentPatterns = listOf("recent", "latest", "new", "recently", "lately")
            if (recentPatterns.any { it in lowerQuery }) {
                return SemanticIntent.FIND_RECENT
            }
        }
        
        return SemanticIntent.UNKNOWN
    }
    
    /**
     * Extract date range from natural language
     */
    private fun extractDateRange(query: String): DateRange? {
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        return when {
            query.contains("today") -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                val startOfDay = calendar.timeInMillis
                DateRange(startOfDay, now, RelativeDateType.TODAY)
            }
            query.contains("yesterday") -> {
                calendar.add(Calendar.DAY_OF_YEAR, -1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfYesterday = calendar.timeInMillis
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val endOfYesterday = calendar.timeInMillis
                DateRange(startOfYesterday, endOfYesterday, RelativeDateType.YESTERDAY)
            }
            query.contains("this week") -> {
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfWeek = calendar.timeInMillis
                DateRange(startOfWeek, now, RelativeDateType.THIS_WEEK)
            }
            query.contains("last week") -> {
                calendar.add(Calendar.WEEK_OF_YEAR, -1)
                calendar.set(Calendar.DAY_OF_WEEK, calendar.firstDayOfWeek)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfLastWeek = calendar.timeInMillis
                calendar.add(Calendar.DAY_OF_YEAR, 6)
                calendar.set(Calendar.HOUR_OF_DAY, 23)
                calendar.set(Calendar.MINUTE, 59)
                val endOfLastWeek = calendar.timeInMillis
                DateRange(startOfLastWeek, endOfLastWeek, RelativeDateType.LAST_WEEK)
            }
            query.contains("this month") -> {
                calendar.set(Calendar.DAY_OF_MONTH, 1)
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                val startOfMonth = calendar.timeInMillis
                DateRange(startOfMonth, now, RelativeDateType.THIS_MONTH)
            }
            else -> null
        }
    }
    
    /**
     * Extract categories from query
     */
    private fun extractCategories(query: String): List<String> {
        val categories = mutableListOf<String>()
        val categoryKeywords = mapOf(
            "work" to listOf("work", "business", "office", "meeting", "project"),
            "personal" to listOf("personal", "private", "diary", "journal"),
            "academic" to listOf("study", "school", "university", "research", "academic"),
            "creative" to listOf("creative", "art", "design", "writing", "ideas"),
            "health" to listOf("health", "fitness", "medical", "doctor", "exercise"),
            "finance" to listOf("money", "budget", "finance", "expense", "income"),
            "travel" to listOf("travel", "trip", "vacation", "flight", "hotel")
        )
        
        categoryKeywords.forEach { (category, keywords) ->
            if (keywords.any { query.contains(it) }) {
                categories.add(category)
            }
        }
        
        return categories
    }
    
    /**
     * Extract tags from query (words starting with #)
     */
    private fun extractTags(query: String): List<String> {
        val tagRegex = Regex("#(\\w+)")
        return tagRegex.findAll(query).map { it.groupValues[1] }.toList()
    }
    
    /**
     * Extract note types from query
     */
    private fun extractNoteTypes(query: String): List<NoteType> {
        val types = mutableListOf<NoteType>()
        
        when {
            query.contains(Regex("(voice|audio|recording)")) -> types.add(NoteType.VOICE)
            query.contains(Regex("(image|photo|picture)")) -> types.add(NoteType.IMAGE)
            query.contains(Regex("(drawing|sketch|draw)")) -> types.add(NoteType.DRAWING)
            query.contains(Regex("(document|pdf|file)")) -> types.add(NoteType.DOCUMENT)
            query.contains(Regex("(checklist|todo|task)")) -> types.add(NoteType.CHECKLIST)
            query.contains(Regex("(meeting|minutes)")) -> types.add(NoteType.MEETING)
            query.contains(Regex("(journal|diary)")) -> types.add(NoteType.JOURNAL)
        }
        
        return types
    }
    
    /**
     * Determine search type based on query characteristics
     */
    private fun determineSearchType(query: String): SearchType {
        return when {
            query.startsWith("\"") && query.endsWith("\"") -> SearchType.EXACT_PHRASE
            query.contains(Regex("\\[|\\]|\\*|\\+|\\?|\\^|\\$|\\{|\\}|\\(|\\)|\\||\\\\")) -> SearchType.REGEX
            query.contains("~") -> SearchType.FUZZY
            query.contains(Regex("(AND|OR|NOT|\\+|-)")) -> SearchType.ADVANCED
            query.contains(Regex("(similar|like|related)")) -> SearchType.SEMANTIC
            else -> SearchType.GENERAL
        }
    }
    
    /**
     * Extract clean search terms from query with advanced filtering
     */
    private fun extractSearchTerms(
        query: String,
        categories: List<String> = emptyList(),
        tags: List<String> = emptyList(),
        noteTypes: List<NoteType> = emptyList()
    ): List<String> {
        // Enhanced stop words list with common filter indicators
        val stopWords = setOf(
            "a", "an", "the", "and", "or", "in", "on", "at", "to", "for", "of", "with",
            "find", "search", "show", "me", "all", "my", "that", "this", "these", "those",
            "from", "since", "between", "before", "after", "on", "last", "next", "this",
            "type", "kind", "format", "with", "without", "has", "have", "had", "containing",
            "about", "regarding", "related", "similar", "like", "important", "pinned",
            "starred", "todo", "unfinished", "incomplete", "pending", "location", "place"
        )
        
        // Filter keywords that should be removed from search terms
        val filterKeywords = setOf(
            "category", "tag", "type", "kind", "format", "from", "since", "between",
            "before", "after", "on", "last", "next", "this", "with", "without",
            "has", "have", "had", "containing", "about", "regarding", "related",
            "similar", "like", "important", "pinned", "starred", "todo",
            "unfinished", "incomplete", "pending", "location", "place"
        )
        
        // Remove filter patterns from query
        var cleanedQuery = query
            .replace(Regex("\\b(category|tag|type|kind|format|from|since|between|before|after|on|last|next|this|with|without|has|have|had|containing|about|regarding|related|similar|like|important|pinned|starred|todo|unfinished|incomplete|pending|location|place)\\s*:\\s*[^\\s]+"), "")
            .replace(Regex("\\b(and|or|not)\\b"), "")
        
        // Remove extracted categories, tags, and note types
        categories.forEach { cleanedQuery = cleanedQuery.replace(it, "", true) }
        tags.forEach { cleanedQuery = cleanedQuery.replace("#$it", "", true) }
        noteTypes.forEach { cleanedQuery = cleanedQuery.replace(it.name.lowercase(), "", true) }
        
        // Extract and clean terms
        return cleanedQuery.split("\\s+")
            .map { it.trim().trim('"', '\'', ',', '.', '!', '?', ':', ';') }
            .filter { it.isNotBlank() && it.length > 1 && it !in stopWords }
            .distinct()
            .filter { it.isNotBlank() && it !in filterKeywords && !it.startsWith("#") }
            .map { it.replace(Regex("[^\\w]"), "") }
            .filter { it.length > 2 }
    }
    
    /**
     * Build processed query for search execution
     */
    private fun buildProcessedQuery(searchTerms: List<String>, intent: SemanticIntent): String {
        return when (intent) {
            SemanticIntent.FIND_RECENT -> searchTerms.joinToString(" ") + " [RECENT]"
            SemanticIntent.FIND_BY_TOPIC -> searchTerms.joinToString(" ") + " [TOPIC]"
            SemanticIntent.FIND_RELATED -> searchTerms.joinToString(" ") + " [RELATED]"
            else -> searchTerms.joinToString(" ")
        }
    }
    
    /**
     * Perform general search with TF-IDF scoring
     */
    private fun performGeneralSearch(query: SmartSearchQuery, notes: List<Note>): List<SmartSearchResult> {
        return notes.mapNotNull { note ->
            val relevanceScore = calculateRelevanceScore(note, query)
            if (relevanceScore > 0.1f) {
                SmartSearchResult(
                    note = note,
                    relevanceScore = relevanceScore,
                    matchedFields = findMatchedFields(note, query),
                    highlights = generateHighlights(note, query),
                    contextSnippets = extractContextSnippets(note, query)
                )
            } else null
        }
    }
    
    /**
     * Perform semantic search using embeddings and similarity
     */
    private fun performSemanticSearch(query: SmartSearchQuery, notes: List<Note>): List<SmartSearchResult> {
        // For now, implement a simplified semantic search
        // In production, this would use actual embeddings and vector similarity
        return performGeneralSearch(query, notes).map { result ->
            result.copy(
                semanticSimilarity = calculateSemanticSimilarity(result.note, query),
                relevanceScore = result.relevanceScore * 1.2f // Boost semantic results
            )
        }
    }
    
    /**
     * Stubs for specialized search modes to avoid unresolved references.
     */
    private fun performExactPhraseSearch(query: SmartSearchQuery, notes: List<Note>): List<SmartSearchResult> =
        performGeneralSearch(query, notes)

    private fun performFuzzySearch(query: SmartSearchQuery, notes: List<Note>): List<SmartSearchResult> =
        performGeneralSearch(query, notes)

    private fun performRegexSearch(query: SmartSearchQuery, notes: List<Note>): List<SmartSearchResult> =
        performGeneralSearch(query, notes)

    private fun performAdvancedSearch(query: SmartSearchQuery, notes: List<Note>): List<SmartSearchResult> =
        performGeneralSearch(query, notes)
    
    /**
     * Calculate relevance score using TF-IDF and other factors
     */
    private fun calculateRelevanceScore(note: Note, query: SmartSearchQuery): Float {
        var score = 0f
        val searchTerms = query.searchTerms
        
        if (searchTerms.isEmpty()) return 0f
        
        // Title matching (higher weight)
        val titleScore = calculateFieldScore(note.title, searchTerms) * 3f
        score += titleScore
        
        // Content matching
        val contentScore = calculateFieldScore(note.content, searchTerms) * 1f
        score += contentScore
        
        // Tag matching (high weight)
        val tagScore = note.tags.sumOf { tag ->
            calculateFieldScore(tag, searchTerms).toDouble()
        }.toFloat() * 2f
        score += tagScore
        
        // Category matching
        val categoryScore = calculateFieldScore(note.category, searchTerms) * 1.5f
        score += categoryScore
        
        // Recency boost
        val daysSinceModified = (System.currentTimeMillis() - note.dateModified) / (1000 * 60 * 60 * 24)
        val recencyBoost = max(0f, 1f - (daysSinceModified / 30f)) * 0.2f
        score += recencyBoost
        
        // Pinned boost
        if (note.isPinned) {
            score *= 1.3f
        }
        
        return min(score, 10f) // Cap at 10
    }
    
    /**
     * Calculate field-specific matching score
     */
    private fun calculateFieldScore(field: String, searchTerms: List<String>): Float {
        if (field.isBlank() || searchTerms.isEmpty()) return 0f
        
        val fieldLower = field.lowercase()
        var score = 0f
        
        searchTerms.forEach { term ->
            val termLower = term.lowercase()
            
            // Exact match
            if (fieldLower.contains(termLower)) {
                score += 1f
                
                // Word boundary match (higher score)
                if (fieldLower.contains("\\b$termLower\\b".toRegex())) {
                    score += 0.5f
                }
                
                // Start of field match (even higher score)
                if (fieldLower.startsWith(termLower)) {
                    score += 0.3f
                }
            }
            
            // Fuzzy match
            val fuzzyScore = calculateFuzzyMatch(fieldLower, termLower)
            score += fuzzyScore * 0.3f
        }
        
        return score
    }
    
    /**
     * Calculate fuzzy matching score using Levenshtein distance
     */
    private fun calculateFuzzyMatch(text: String, term: String): Float {
        if (term.length < 3) return 0f
        
        val words = text.split(Regex("\\s+"))
        var bestScore = 0f
        
        words.forEach { word ->
            if (word.length >= term.length - 1) {
                val distance = levenshteinDistance(word, term)
                val similarity = 1f - (distance.toFloat() / max(word.length, term.length))
                if (similarity > 0.7f) {
                    bestScore = max(bestScore, similarity)
                }
            }
        }
        
        return bestScore
    }
    
    /**
     * Calculate Levenshtein distance between two strings
     */
    private fun levenshteinDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }
        
        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j
        
        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                dp[i][j] = if (s1[i - 1] == s2[j - 1]) {
                    dp[i - 1][j - 1]
                } else {
                    1 + minOf(dp[i - 1][j], dp[i][j - 1], dp[i - 1][j - 1])
                }
            }
        }
        
        return dp[s1.length][s2.length]
    }
    
    /**
     * Find matched fields in the note
     */
    private fun findMatchedFields(note: Note, query: SmartSearchQuery): List<MatchedField> {
        val matchedFields = mutableListOf<MatchedField>()
        
        // Check title
        val titleScore = calculateFieldScore(note.title, query.searchTerms)
        if (titleScore > 0) {
            matchedFields.add(
                MatchedField(
                    fieldName = "title",
                    fieldType = FieldType.TITLE,
                    matchStrength = titleScore,
                    exactMatch = query.searchTerms.any { note.title.lowercase().contains(it.lowercase()) }
                )
            )
        }
        
        // Check content
        val contentScore = calculateFieldScore(note.content, query.searchTerms)
        if (contentScore > 0) {
            matchedFields.add(
                MatchedField(
                    fieldName = "content",
                    fieldType = FieldType.CONTENT,
                    matchStrength = contentScore,
                    exactMatch = query.searchTerms.any { note.content.lowercase().contains(it.lowercase()) }
                )
            )
        }
        
        // Check tags
        note.tags.forEach { tag ->
            val tagScore = calculateFieldScore(tag, query.searchTerms)
            if (tagScore > 0) {
                matchedFields.add(
                    MatchedField(
                        fieldName = tag,
                        fieldType = FieldType.TAGS,
                        matchStrength = tagScore,
                        exactMatch = query.searchTerms.any { tag.lowercase().contains(it.lowercase()) }
                    )
                )
            }
        }
        
        return matchedFields
    }
    
    /**
     * Generate search highlights for matched terms
     */
    private fun generateHighlights(note: Note, query: SmartSearchQuery): List<SearchHighlight> {
        val highlights = mutableListOf<SearchHighlight>()
        
        query.searchTerms.forEach { term ->
            // Highlight in title
            findHighlightsInText(note.title, term, FieldType.TITLE).forEach { highlight ->
                highlights.add(highlight)
            }
            
            // Highlight in content
            findHighlightsInText(note.content, term, FieldType.CONTENT).forEach { highlight ->
                highlights.add(highlight)
            }
        }
        
        return highlights
    }
    
    /**
     * Find highlights in specific text field
     */
    private fun findHighlightsInText(text: String, term: String, fieldType: FieldType): List<SearchHighlight> {
        val highlights = mutableListOf<SearchHighlight>()
        val termLower = term.lowercase()
        val textLower = text.lowercase()
        
        var startIndex = 0
        while (true) {
            val index = textLower.indexOf(termLower, startIndex)
            if (index == -1) break
            
            highlights.add(
                SearchHighlight(
                    text = text.substring(index, index + term.length),
                    startIndex = index,
                    endIndex = index + term.length,
                    fieldType = fieldType,
                    matchType = HighlightType.EXACT_MATCH
                )
            )
            
            startIndex = index + 1
        }
        
        return highlights
    }
    
    /**
     * Extract context snippets around matched terms
     */
    private fun extractContextSnippets(note: Note, query: SmartSearchQuery): List<String> {
        val snippets = mutableListOf<String>()
        val content = note.content
        val snippetLength = 150
        
        query.searchTerms.forEach { term ->
            val index = content.lowercase().indexOf(term.lowercase())
            if (index != -1) {
                val start = max(0, index - snippetLength / 2)
                val end = min(content.length, index + term.length + snippetLength / 2)
                val snippet = content.substring(start, end).trim()
                
                if (snippet.isNotBlank() && snippet !in snippets) {
                    snippets.add(snippet)
                }
            }
        }
        
        return snippets.take(3) // Limit to 3 snippets
    }
    
    /**
     * Calculate semantic similarity (simplified implementation)
     */
    private fun calculateSemanticSimilarity(note: Note, query: SmartSearchQuery): Float {
        // This is a simplified implementation
        // In production, you would use actual word embeddings or sentence transformers
        val noteWords = (note.title + " " + note.content).lowercase().split(Regex("\\s+")).toSet()
        val queryWords = query.searchTerms.toSet()
        
        val intersection = noteWords.intersect(queryWords).size
        val union = noteWords.union(queryWords).size
        
        return if (union > 0) intersection.toFloat() / union else 0f
    }
    
    /**
     * Apply filters to search results
     */
    private fun applyFilters(results: List<SmartSearchResult>, filters: SearchFilters): List<SmartSearchResult> {
        return results.filter { result ->
            val note = result.note
            
            // Date range filter
            filters.dateRange?.let { dateRange ->
            if (note.dateModified < dateRange.startDate || note.dateModified > dateRange.endDate) {
                return@filter false
            }
            }
            
            // Also respect last-modified range if provided
            filters.lastModifiedRange?.let { dateRange ->
                if (note.dateModified < dateRange.startDate || note.dateModified > dateRange.endDate) {
                    return@filter false
                }
            }

            // Category filter
            if (filters.categories.isNotEmpty() && note.category !in filters.categories) {
                return@filter false
            }

            // Tag filter (any match)
            if (filters.tags.isNotEmpty() && !note.tags.any { it in filters.tags }) {
                return@filter false
            }

            // Pinned filter
            filters.isPinned?.let { requiredPinned ->
                if (note.isPinned != requiredPinned) return@filter false
            }

            // Length filters (by word count of content)
            val wordCount = note.content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }.size
            filters.minLength?.let { minLen ->
                if (wordCount < minLen) return@filter false
            }
            filters.maxLength?.let { maxLen ->
                if (wordCount > maxLen) return@filter false
            }

            true
        }
    }
    
    /**
     * Generate search suggestions based on query and results
     */
    private fun generateSearchSuggestions(query: SmartSearchQuery, results: List<SmartSearchResult>): List<SearchSuggestion> {
        val suggestions = mutableListOf<SearchSuggestion>()

        // Tag-based suggestions from top result tags
        val tagCounts = results.flatMap { it.note.tags }.groupingBy { it }.eachCount()
        tagCounts.entries.sortedByDescending { it.value }.take(3).forEach { entry ->
            suggestions.add(
                SearchSuggestion(
                    suggestion = "#${entry.key}",
                    type = SuggestionType.FILTER_SUGGESTION,
                    confidence = 0.5f
                )
            )
        }

        // Simple refinements driven by detected intent
        when (query.semanticIntent) {
            SemanticIntent.FIND_RECENT -> {
                suggestions.add(
                    SearchSuggestion(
                        suggestion = "${query.processedQuery} from this week",
                        type = SuggestionType.QUERY_REFINEMENT,
                        confidence = 0.6f
                    )
                )
                suggestions.add(
                    SearchSuggestion(
                        suggestion = "${query.processedQuery} this month",
                        type = SuggestionType.QUERY_REFINEMENT,
                        confidence = 0.5f
                    )
                )
            }
            SemanticIntent.FIND_BY_TOPIC -> {
                suggestions.add(
                    SearchSuggestion(
                        suggestion = "${query.processedQuery} related",
                        type = SuggestionType.SEMANTIC_EXPANSION,
                        confidence = 0.5f
                    )
                )
            }
            else -> {
                suggestions.add(
                    SearchSuggestion(
                        suggestion = "${query.processedQuery} important",
                        type = SuggestionType.QUERY_REFINEMENT,
                        confidence = 0.4f
                    )
                )
            }
        }

        return suggestions.distinctBy { it.suggestion }
    }

    /**
     * Public suggestions API for partial queries.
     */
    fun getSuggestions(query: String): List<SearchSuggestion> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val suggestions = mutableListOf<SearchSuggestion>()
        // Simple completions and refinements
        suggestions += SearchSuggestion("$q today", SuggestionType.QUERY_REFINEMENT, 0.6f)
        suggestions += SearchSuggestion("$q last week", SuggestionType.QUERY_REFINEMENT, 0.5f)
        if (!q.startsWith("#")) {
            suggestions += SearchSuggestion("#$q", SuggestionType.FILTER_SUGGESTION, 0.5f)
        }
        return suggestions.distinctBy { it.suggestion }
    }

    /**
     * Record search query for analytics
     */
    private fun recordSearchQuery(query: SmartSearchQuery) {
        // Implementation for search analytics
        // This would track popular searches, failed searches, etc.
    }
}

/**
 * Search results container
 */
data class SearchResults(
    val query: SmartSearchQuery,
    val results: List<SmartSearchResult>,
    val totalResults: Int,
    val searchTime: Long,
    val fromCache: Boolean = false,
    val suggestions: List<SearchSuggestion> = emptyList()
)

/**
 * Search index entry for performance optimization (local-only to avoid clash with SearchIndexManager)
 */
private data class LocalSearchIndexEntry(
    val noteId: String,
    val titleWords: Set<String>,
    val contentWords: Set<String>,
    val tags: Set<String>,
    val lastIndexed: Long
)