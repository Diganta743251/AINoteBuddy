package com.ainotebuddy.app.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.ainotebuddy.app.data.Note
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

// DataStore extension
private val Context.searchIndexDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_index")

/**
 * High-performance search index manager with caching and optimization
 */
class SearchIndexManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // In-memory search index for fast lookups
    private val searchIndex = ConcurrentHashMap<String, SearchIndexEntry>()
    private val wordIndex = ConcurrentHashMap<String, MutableSet<String>>() // word -> note IDs
    private val categoryIndex = ConcurrentHashMap<String, MutableSet<String>>() // category -> note IDs
    private val tagIndex = ConcurrentHashMap<String, MutableSet<String>>() // tag -> note IDs
    
    // Query cache for performance
    private val queryCache = ConcurrentHashMap<String, CachedSearchResult>()
    private val maxCacheSize = 200
    private val cacheExpirationTime = 5 * 60 * 1000L // 5 minutes
    
    // Index statistics
    private val _indexStats = MutableStateFlow(SearchIndexStats())
    val indexStats: StateFlow<SearchIndexStats> = _indexStats.asStateFlow()
    
    // DataStore keys
    private val indexDataKey = stringPreferencesKey("search_index_data")
    private val indexStatsKey = stringPreferencesKey("search_index_stats")
    
    init {
        loadSearchIndex()
    }
    
    /**
     * Build or rebuild the search index from notes
     */
    suspend fun buildIndex(notes: List<Note>) = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        
        // Clear existing index
        searchIndex.clear()
        wordIndex.clear()
        categoryIndex.clear()
        tagIndex.clear()
        
        // Build new index
        notes.forEach { note ->
            indexNote(note)
        }
        
        val buildTime = System.currentTimeMillis() - startTime
        
        // Update statistics
        val stats = SearchIndexStats(
            totalNotes = notes.size,
            totalWords = wordIndex.size,
            totalCategories = categoryIndex.size,
            totalTags = tagIndex.size,
            lastIndexed = System.currentTimeMillis(),
            indexBuildTime = buildTime,
            indexSize = calculateIndexSize()
        )
        
        _indexStats.value = stats
        // Persist index
        saveSearchIndex()
        
        // Clear query cache since index changed
        queryCache.clear()
    }
    /**
     * Add or update a single note in the index
     */
    suspend fun indexNote(note: Note) = withContext(Dispatchers.IO) {
        // Remove existing entry if it exists
        removeNoteFromIndex(note.id.toString())
        
        // Create new index entry
        val titleWords = extractWords(note.title)
        val contentWords = extractWords(note.content)
        val allWords = (titleWords + contentWords).toSet()
        
        val indexEntry = SearchIndexEntry(
            noteId = note.id.toString(),
            titleWords = titleWords,
            contentWords = contentWords,
            allWords = allWords,
            category = note.category,
            tags = note.tags.toSet(),
            lastModified = note.dateModified,
            wordCount = note.content.split("\\s+".toRegex()).size,
            lastIndexed = System.currentTimeMillis()
        )
        
        searchIndex[note.id.toString()] = indexEntry
        
        // Update word index
        allWords.forEach { word ->
            wordIndex.getOrPut(word) { mutableSetOf() }.add(note.id.toString())
        }
        
        // Update category index
        if (note.category.isNotBlank()) {
            categoryIndex.getOrPut(note.category) { mutableSetOf() }.add(note.id.toString())
        }
        
        // Update tag index
        note.tags.forEach { tag ->
            tagIndex.getOrPut(tag) { mutableSetOf() }.add(note.id.toString())
        }
        
        // Clear related cache entries
        clearRelatedCacheEntries(allWords)
    }
    
    /**
     * Remove a note from the index
     */
    suspend fun removeNoteFromIndex(noteId: String) = withContext(Dispatchers.IO) {
        val existingEntry = searchIndex.remove(noteId) ?: return@withContext
        
        // Remove from word index
        existingEntry.allWords.forEach { word ->
            wordIndex[word]?.remove(noteId)
            if (wordIndex[word]?.isEmpty() == true) {
                wordIndex.remove(word)
            }
        }
        
        // Remove from category index
        categoryIndex[existingEntry.category]?.remove(noteId)
        if (categoryIndex[existingEntry.category]?.isEmpty() == true) {
            categoryIndex.remove(existingEntry.category)
        }
        
        // Remove from tag index
        existingEntry.tags.forEach { tag ->
            tagIndex[tag]?.remove(noteId)
            if (tagIndex[tag]?.isEmpty() == true) {
                tagIndex.remove(tag)
            }
        }
        
        // Clear cache
        queryCache.clear()
    }
    
    /**
     * Fast search using the index
     */
    suspend fun searchIndex(
        query: SmartSearchQuery,
        maxResults: Int = 50
    ): List<IndexSearchResult> = withContext(Dispatchers.IO) {
        val cacheKey = "${query.processedQuery}_${query.filters.hashCode()}_$maxResults"
        
        // Check cache first
        val cachedResult = queryCache[cacheKey]
        if (cachedResult != null && !cachedResult.isExpired()) {
            return@withContext cachedResult.results
        }
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<IndexSearchResult>()
        
        // Get candidate note IDs based on search terms
        val candidateIds = if (query.searchTerms.isEmpty()) {
            searchIndex.keys
        } else {
            getCandidateNoteIds(query.searchTerms)
        }
        
        // Score and filter candidates
        candidateIds.forEach { noteId ->
            val indexEntry = searchIndex[noteId] ?: return@forEach
            
            // Apply filters
            if (!matchesFilters(indexEntry, query.filters)) return@forEach
            
            // Calculate relevance score
            val score = calculateIndexScore(indexEntry, query)
            if (score > 0.1f) {
                results.add(
                    IndexSearchResult(
                        noteId = noteId,
                        score = score,
                        titleMatches = findMatches(indexEntry.titleWords, query.searchTerms),
                        contentMatches = findMatches(indexEntry.contentWords, query.searchTerms),
                        categoryMatch = indexEntry.category in query.filters.categories,
                        tagMatches = indexEntry.tags.intersect(query.filters.tags.toSet())
                    )
                )
            }
        }
        
        // Sort by score and limit results
        val sortedResults = results.sortedByDescending { it.score }.take(maxResults)
        
        // Cache results
        val searchTime = System.currentTimeMillis() - startTime
        cacheSearchResults(cacheKey, sortedResults, searchTime)
        
        sortedResults
    }
    
    /**
     * Get search suggestions based on index
     */
    fun getIndexSuggestions(partialQuery: String, limit: Int = 10): List<String> {
        val suggestions = mutableSetOf<String>()
        val queryLower = partialQuery.lowercase()
        
        // Word completions
        wordIndex.keys
            .filter { it.startsWith(queryLower) && it.length > queryLower.length }
            .sortedBy { it.length }
            .take(limit / 2)
            .forEach { suggestions.add(it) }
        
        // Category suggestions
        categoryIndex.keys
            .filter { it.lowercase().contains(queryLower) }
            .take(2)
            .forEach { suggestions.add("in $it") }
        
        // Tag suggestions
        tagIndex.keys
            .filter { it.lowercase().contains(queryLower) }
            .take(2)
            .forEach { suggestions.add("#$it") }
        
        return suggestions.take(limit).toList()
    }
    
    /**
     * Get index statistics and health metrics
     */
    fun getIndexHealth(): IndexHealthMetrics {
        val stats = _indexStats.value
        val cacheHitRate = calculateCacheHitRate()
        val averageWordsPerNote = if (stats.totalNotes > 0) stats.totalWords.toFloat() / stats.totalNotes else 0f
        
        return IndexHealthMetrics(
            isHealthy = stats.totalNotes > 0 && stats.lastIndexed > 0,
            indexAge = System.currentTimeMillis() - stats.lastIndexed,
            cacheHitRate = cacheHitRate,
            averageWordsPerNote = averageWordsPerNote,
            memoryUsage = calculateMemoryUsage(),
            recommendedActions = generateHealthRecommendations(stats, cacheHitRate)
        )
    }
    
    /**
     * Optimize the search index
     */
    suspend fun optimizeIndex() = withContext(Dispatchers.IO) {
        // Clean up empty entries
        wordIndex.entries.removeAll { it.value.isEmpty() }
        categoryIndex.entries.removeAll { it.value.isEmpty() }
        tagIndex.entries.removeAll { it.value.isEmpty() }
        
        // Clean expired cache entries
        val currentTime = System.currentTimeMillis()
        queryCache.entries.removeAll { it.value.isExpired(currentTime) }
        
        // Limit cache size
        if (queryCache.size > maxCacheSize) {
            val entriesToRemove = queryCache.size - maxCacheSize
            queryCache.entries
                .sortedBy { it.value.timestamp }
                .take(entriesToRemove)
                .forEach { queryCache.remove(it.key) }
        }
        
        // Update statistics
        val stats = _indexStats.value.copy(
            lastOptimized = currentTime,
            cacheSize = queryCache.size
        )
        _indexStats.value = stats
        
        saveSearchIndex()
    }
    
    // Private helper methods
    private fun extractWords(text: String): Set<String> {
        return text.lowercase()
            .split(Regex("[\\s\\p{Punct}]+"))
            .filter { it.length > 2 }
            .toSet()
    }
    
    private fun getCandidateNoteIds(searchTerms: List<String>): Set<String> {
        if (searchTerms.isEmpty()) return searchIndex.keys.toSet()
        
        val termResults = searchTerms.map { term ->
            wordIndex[term.lowercase()] ?: emptySet()
        }
        
        // Return intersection of all terms (AND logic)
        return if (termResults.isNotEmpty()) {
            termResults.reduce { acc, set -> acc.intersect(set) }
        } else {
            emptySet()
        }
    }
    
    private fun matchesFilters(entry: SearchIndexEntry, filters: SearchFilters): Boolean {
        // Date range filter
        filters.dateRange?.let { dateRange ->
            if (entry.lastModified < dateRange.startDate || entry.lastModified > dateRange.endDate) {
                return false
            }
        }
        
        // Category filter
        if (filters.categories.isNotEmpty() && entry.category !in filters.categories) {
            return false
        }
        
        // Tag filter
        if (filters.tags.isNotEmpty() && !entry.tags.any { it in filters.tags }) {
            return false
        }
        
        // Length filters
        filters.minLength?.let { minLength ->
            if (entry.wordCount < minLength) return false
        }
        
        filters.maxLength?.let { maxLength ->
            if (entry.wordCount > maxLength) return false
        }
        
        return true
    }
    
    private fun calculateIndexScore(entry: SearchIndexEntry, query: SmartSearchQuery): Float {
        var score = 0f
        
        query.searchTerms.forEach { term ->
            val termLower = term.lowercase()
            
            // Title matches (higher weight)
            if (termLower in entry.titleWords) {
                score += 3f
            }
            
            // Content matches
            if (termLower in entry.contentWords) {
                score += 1f
            }
            
            // Fuzzy matches in title
            entry.titleWords.forEach { titleWord ->
                if (titleWord.contains(termLower) || termLower.contains(titleWord)) {
                    score += 1.5f
                }
            }
        }
        
        // Category boost
        if (entry.category in query.filters.categories) {
            score += 2f
        }
        
        // Tag boost
        entry.tags.forEach { tag ->
            if (tag in query.filters.tags) {
                score += 2f
            }
        }
        
        // Recency boost
        val daysSinceModified = (System.currentTimeMillis() - entry.lastModified) / (1000 * 60 * 60 * 24)
        val recencyBoost = kotlin.math.max(0f, 1f - (daysSinceModified / 30f)) * 0.5f
        score += recencyBoost
        
        return score
    }
    
    private fun findMatches(words: Set<String>, searchTerms: List<String>): Set<String> {
        return words.filter { word ->
            searchTerms.any { term ->
                word.contains(term.lowercase()) || term.lowercase().contains(word)
            }
        }.toSet()
    }
    
    private fun cacheSearchResults(
        cacheKey: String,
        results: List<IndexSearchResult>,
        searchTime: Long
    ) {
        if (queryCache.size >= maxCacheSize) {
            // Remove oldest entry
            val oldestEntry = queryCache.minByOrNull { it.value.timestamp }
            oldestEntry?.let { queryCache.remove(it.key) }
        }
        
        queryCache[cacheKey] = CachedSearchResult(
            results = results,
            timestamp = System.currentTimeMillis(),
            searchTime = searchTime
        )
    }
    
    private fun clearRelatedCacheEntries(words: Set<String>) {
        val keysToRemove = queryCache.keys.filter { cacheKey ->
            words.any { word -> cacheKey.contains(word, ignoreCase = true) }
        }
        keysToRemove.forEach { queryCache.remove(it) }
    }
    
    private fun calculateIndexSize(): Long {
        return (searchIndex.size * 100 + // Rough estimate
                wordIndex.size * 50 +
                categoryIndex.size * 20 +
                tagIndex.size * 20).toLong()
    }
    
    private fun calculateCacheHitRate(): Float {
        // This would be tracked in a real implementation
        return 0.75f // Placeholder
    }
    
    private fun calculateMemoryUsage(): Long {
        // Rough estimate of memory usage
        return calculateIndexSize() + queryCache.size * 200L
    }
    
    private fun generateHealthRecommendations(
        stats: SearchIndexStats,
        cacheHitRate: Float
    ): List<String> {
        val recommendations = mutableListOf<String>()
        
        val indexAge = System.currentTimeMillis() - stats.lastIndexed
        if (indexAge > 24 * 60 * 60 * 1000) { // 24 hours
            recommendations.add("Index is outdated, consider rebuilding")
        }
        
        if (cacheHitRate < 0.5f) {
            recommendations.add("Low cache hit rate, consider optimizing queries")
        }
        
        if (stats.indexBuildTime > 5000) { // 5 seconds
            recommendations.add("Index build time is high, consider optimization")
        }
        
        return recommendations
    }
    
    private fun loadSearchIndex() {
        scope.launch {
            context.searchIndexDataStore.data.collect { preferences ->
                preferences[indexStatsKey]?.let { jsonStr ->
                    try {
                        val stats = json.decodeFromString<SearchIndexStats>(jsonStr)
                        val newStats = SearchIndexStats(
                            totalNotes = stats.totalNotes,
                            totalWords = stats.totalWords,
                            totalCategories = stats.totalCategories,
                            totalTags = stats.totalTags,
                            lastIndexed = stats.lastIndexed,
                            lastOptimized = stats.lastOptimized,
                            indexBuildTime = stats.indexBuildTime,
                            indexSize = stats.indexSize,
                            cacheSize = stats.cacheSize
                        )
                        _indexStats.value = newStats
                    } catch (e: Exception) {
                        // Handle error
                    }
                }
            }
        }
    }
    
    private suspend fun saveSearchIndex() {
        context.searchIndexDataStore.edit { preferences ->
            preferences[indexStatsKey] = json.encodeToString(_indexStats.value)
        }
    }
}

// Data classes for search index
@Serializable
data class SearchIndexEntry(
    val noteId: String,
    val titleWords: Set<String>,
    val contentWords: Set<String>,
    val allWords: Set<String>,
    val category: String,
    val tags: Set<String>,
    val lastModified: Long,
    val wordCount: Int,
    val lastIndexed: Long
)

data class IndexSearchResult(
    val noteId: String,
    val score: Float,
    val titleMatches: Set<String>,
    val contentMatches: Set<String>,
    val categoryMatch: Boolean,
    val tagMatches: Set<String>
)

data class CachedSearchResult(
    val results: List<IndexSearchResult>,
    val timestamp: Long,
    val searchTime: Long
) {
    fun isExpired(currentTime: Long = System.currentTimeMillis()): Boolean {
        return currentTime - timestamp > 5 * 60 * 1000 // 5 minutes
    }
}

@Serializable
data class SearchIndexStats(
    val totalNotes: Int = 0,
    val totalWords: Int = 0,
    val totalCategories: Int = 0,
    val totalTags: Int = 0,
    val lastIndexed: Long = 0,
    val lastOptimized: Long = 0,
    val indexBuildTime: Long = 0,
    val indexSize: Long = 0,
    val cacheSize: Int = 0
)

data class IndexHealthMetrics(
    val isHealthy: Boolean,
    val indexAge: Long,
    val cacheHitRate: Float,
    val averageWordsPerNote: Float,
    val memoryUsage: Long,
    val recommendedActions: List<String>
)