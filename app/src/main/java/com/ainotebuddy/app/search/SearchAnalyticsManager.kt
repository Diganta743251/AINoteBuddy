package com.ainotebuddy.app.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json
import java.util.*

private val Context.searchAnalyticsDataStore: DataStore<Preferences> by preferencesDataStore(name = "search_analytics")

/**
 * Comprehensive search analytics and intelligence system
 */
class SearchAnalyticsManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    
    // DataStore keys
    private val analyticsKey = stringPreferencesKey("search_analytics")
    private val queryPatternsKey = stringPreferencesKey("query_patterns")
    private val searchTrendsKey = stringPreferencesKey("search_trends")
    
    // Analytics data
    private val _analytics = MutableStateFlow(SearchAnalytics())
    val analytics: StateFlow<SearchAnalytics> = _analytics.asStateFlow()
    
    private val _queryPatterns = MutableStateFlow<List<QueryPattern>>(emptyList())
    val queryPatterns: StateFlow<List<QueryPattern>> = _queryPatterns.asStateFlow()
    
    private val _searchTrends = MutableStateFlow<List<SearchTrend>>(emptyList())
    val searchTrends: StateFlow<List<SearchTrend>> = _searchTrends.asStateFlow()
    
    // Local aggregates not present in canonical SearchAnalytics
    private var successfulCount: Long = 0L
    private val failedQueries: MutableList<String> = mutableListOf()
    private val popularTerms: MutableMap<String, Int> = mutableMapOf()
    
    init {
        loadAnalytics()
    }
    
    /**
     * Record a search query and its results
     */
    suspend fun recordSearch(
        query: SmartSearchQuery,
        resultCount: Int,
        searchTime: Long,
        wasSuccessful: Boolean
    ) {
        val current = _analytics.value

        // Update local aggregates
        if (wasSuccessful) successfulCount += 1 else failedQueries.add(query.rawQuery)
        query.searchTerms.forEach { term ->
            popularTerms[term] = (popularTerms[term] ?: 0) + 1
        }

        // Update canonical analytics fields
        val newTotal = current.totalSearches + 1
        val newAvgResults = calculateNewAverage(
            current.averageResultsPerSearch,
            current.totalSearches.toInt(),
            resultCount.toFloat()
        )
        val newPerf = current.performanceMetrics.copy(
            averageSearchTime = calculateNewAverageTime(
                current.performanceMetrics.averageSearchTime,
                current.totalSearches.toInt(),
                searchTime
            )
        )
        val updatedTopQueries: List<QueryFrequency> = run {
            val freq = current.topQueries.associateBy({ it.query }, { it }).toMutableMap()
            val existing = freq[query.rawQuery]
            if (existing == null) {
                freq[query.rawQuery] = QueryFrequency(query = query.rawQuery, frequency = 1, lastSearched = System.currentTimeMillis())
            } else {
                freq[query.rawQuery] = existing.copy(
                    frequency = existing.frequency + 1,
                    lastSearched = System.currentTimeMillis()
                )
            }
            freq.values.sortedByDescending { it.frequency }.take(50)
        }

        _analytics.value = current.copy(
            totalSearches = newTotal,
            averageResultsPerSearch = newAvgResults,
            topQueries = updatedTopQueries,
            performanceMetrics = newPerf
        )

        // Update query patterns and trends
        updateQueryPatterns(query, wasSuccessful, resultCount)
        updateSearchTrends(query, resultCount)
        saveAnalytics()
    }
    
    // Private helper methods
    private fun calculateNewAverage(currentAvg: Float, count: Int, newValue: Float): Float {
        return if (count == 0) newValue else (currentAvg * count + newValue) / (count + 1)
    }
    
    private fun calculateNewAverageTime(currentAvg: Long, count: Int, newValue: Long): Long {
        return if (count == 0) newValue else (currentAvg * count + newValue) / (count + 1)
    }
    
    private fun calculateSuccessRate(totalSearches: Int, successfulSearches: Int, currentRate: Float): Float {
        return if (totalSearches == 1) {
            if (successfulSearches == 1) 1.0f else 0.0f
        } else {
            val previousSuccessful = (currentRate * (totalSearches - 1)).toInt()
            (previousSuccessful + successfulSearches).toFloat() / totalSearches
        }
    }
    
    private fun updatePopularTerms(
        currentTerms: Map<String, Int>,
        newTerms: List<String>
    ): Map<String, Int> {
        val updatedTerms = currentTerms.toMutableMap()
        newTerms.forEach { term ->
            updatedTerms[term] = updatedTerms.getOrDefault(term, 0) + 1
        }
        return updatedTerms.toList()
            .sortedByDescending { it.second }
            .take(50)
            .toMap()
    }
    
    private suspend fun updateQueryPatterns(
        query: SmartSearchQuery,
        wasSuccessful: Boolean,
        resultCount: Int
    ) {
        val currentPatterns = _queryPatterns.value.toMutableList()
        val existingIndex = currentPatterns.indexOfFirst { it.query == query.rawQuery }
        
        if (existingIndex != -1) {
            val existing = currentPatterns[existingIndex]
            currentPatterns[existingIndex] = existing.copy(
                usageCount = existing.usageCount + 1,
                successCount = existing.successCount + if (wasSuccessful) 1 else 0,
                successRate = (existing.successCount + if (wasSuccessful) 1 else 0).toFloat() / (existing.usageCount + 1),
                averageResults = calculateNewAverage(existing.averageResults, existing.usageCount, resultCount.toFloat()),
                lastUsed = System.currentTimeMillis()
            )
        } else {
            currentPatterns.add(
                QueryPattern(
                    query = query.rawQuery,
                    searchType = query.searchType,
                    semanticIntent = query.semanticIntent,
                    usageCount = 1,
                    successCount = if (wasSuccessful) 1 else 0,
                    successRate = if (wasSuccessful) 1.0f else 0.0f,
                    averageResults = resultCount.toFloat(),
                    lastUsed = System.currentTimeMillis()
                )
            )
        }
        
        _queryPatterns.value = currentPatterns.sortedByDescending { it.usageCount }.take(100)
    }
    
    private suspend fun updateSearchTrends(query: SmartSearchQuery, resultCount: Int) {
        val currentTrends = _searchTrends.value.toMutableList()
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        
        val todayTrendIndex = currentTrends.indexOfFirst { it.date == today }
        
        if (todayTrendIndex != -1) {
            val existing = currentTrends[todayTrendIndex]
            currentTrends[todayTrendIndex] = existing.copy(
                searchCount = existing.searchCount + 1,
                averageResults = calculateNewAverage(existing.averageResults, existing.searchCount, resultCount.toFloat()),
                topQueries = updateTopQueries(existing.topQueries, query.rawQuery)
            )
        } else {
            currentTrends.add(
                SearchTrend(
                    date = today,
                    searchCount = 1,
                    averageResults = resultCount.toFloat(),
                    topQueries = mapOf(query.rawQuery to 1)
                )
            )
        }
        
        _searchTrends.value = currentTrends.sortedBy { it.date }.takeLast(90)
    }
    
    private fun updateTopQueries(currentQueries: Map<String, Int>, newQuery: String): Map<String, Int> {
        val updated = currentQueries.toMutableMap()
        updated[newQuery] = updated.getOrDefault(newQuery, 0) + 1
        return updated.toList()
            .sortedByDescending { it.second }
            .take(5)
            .toMap()
    }
    
    private fun generateSemanticExpansion(partialQuery: String, pattern: QueryPattern): String {
        return when (pattern.semanticIntent) {
            SemanticIntent.FIND_RECENT -> "$partialQuery from this week"
            SemanticIntent.FIND_BY_TOPIC -> "$partialQuery related notes"
            SemanticIntent.FIND_IMPORTANT -> "$partialQuery important"
            SemanticIntent.FIND_BY_TYPE -> "$partialQuery voice notes"
            else -> "$partialQuery similar"
        }
    }
    
    private fun getPopularRefinements(partialQuery: String): List<SearchSuggestion> {
        val refinements = listOf(
            "from this week",
            "important",
            "#work",
            "voice notes",
            "with images"
        )
        
        return refinements.map { refinement ->
            SearchSuggestion(
                suggestion = "$partialQuery $refinement",
                type = SuggestionType.QUERY_REFINEMENT,
                confidence = 0.6f
            )
        }
    }
    
    private fun analyzeQueryPatterns(patterns: List<QueryPattern>): QueryPatternInsights {
        val totalPatterns = patterns.size
        val successfulPatterns = patterns.count { it.successRate > 0.7f }
        val averageSuccessRate = patterns.map { it.successRate }.average().toFloat()
        
        val intentDistribution = patterns.groupingBy { it.semanticIntent }
            .eachCount()
            .mapValues { it.value.toFloat() / totalPatterns }
        
        val typeDistribution = patterns.groupingBy { it.searchType }
            .eachCount()
            .mapValues { it.value.toFloat() / totalPatterns }
        
        return QueryPatternInsights(
            totalPatterns = totalPatterns,
            successfulPatterns = successfulPatterns,
            averageSuccessRate = averageSuccessRate,
            intentDistribution = intentDistribution,
            typeDistribution = typeDistribution,
            mostSuccessfulIntent = intentDistribution.maxByOrNull { it.value }?.key ?: SemanticIntent.UNKNOWN,
            mostUsedType = typeDistribution.maxByOrNull { it.value }?.key ?: SearchType.GENERAL
        )
    }
    
    private fun generateRecommendations(
        analytics: SearchAnalytics,
        patterns: List<QueryPattern>,
        trends: List<SearchTrend>
    ): List<String> {
        val recommendations = mutableListOf<String>()
        val total = analytics.totalSearches
        val successRate = if (total == 0L) 0f else (successfulCount.toFloat() / total.toFloat())
        if (successRate < 0.6f) {
            recommendations.add("Try using more specific terms in your searches")
        }
        
        if (analytics.averageResultsPerSearch < 2f) {
            recommendations.add("Use broader terms or try semantic search")
        }
        
        if (patterns.none { it.semanticIntent == SemanticIntent.FIND_RECENT }) {
            recommendations.add("Try searching by date: 'notes from this week'")
        }
        
        if (trends.isNotEmpty() && trends.last().searchCount > 10) {
            recommendations.add("Consider saving your frequent searches as presets")
        }
        
        return recommendations
    }
    
    private fun loadAnalytics() {
        // Load analytics from DataStore
        context.searchAnalyticsDataStore.data
            .map { preferences ->
                preferences[analyticsKey]?.let { jsonStr ->
                    try {
                        json.decodeFromString<SearchAnalytics>(jsonStr)
                    } catch (e: Exception) {
                        SearchAnalytics()
                    }
                } ?: SearchAnalytics()
            }
            
            .onEach { analytics ->
                _analytics.value = analytics
            }
            .launchIn(kotlinx.coroutines.GlobalScope)
        
        // Load query patterns
        context.searchAnalyticsDataStore.data
            .map { preferences ->
                preferences[queryPatternsKey]?.let { jsonStr ->
                    try {
                        json.decodeFromString<List<QueryPattern>>(jsonStr)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            }
            
            .onEach { patterns ->
                _queryPatterns.value = patterns
            }
            .launchIn(kotlinx.coroutines.GlobalScope)
        
        // Load search trends
        context.searchAnalyticsDataStore.data
            .map { preferences ->
                preferences[searchTrendsKey]?.let { jsonStr ->
                    try {
                        json.decodeFromString<List<SearchTrend>>(jsonStr)
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            }
            
            .onEach { trends ->
                _searchTrends.value = trends
            }
            .launchIn(kotlinx.coroutines.GlobalScope)
    }
    private suspend fun saveAnalytics() {
        context.searchAnalyticsDataStore.edit { preferences ->
            preferences[analyticsKey] = json.encodeToString(_analytics.value)
            preferences[queryPatternsKey] = json.encodeToString(_queryPatterns.value)
            preferences[searchTrendsKey] = json.encodeToString(_searchTrends.value)
        }
    }
}

// Data classes for analytics
@Serializable
data class QueryPattern(
    val query: String,
    val searchType: SearchType,
    val semanticIntent: SemanticIntent,
    val usageCount: Int,
    val successCount: Int,
    val successRate: Float,
    val averageResults: Float,
    val lastUsed: Long
)

@Serializable
data class SearchTrend(
    val date: Long,
    val searchCount: Int,
    val averageResults: Float,
    val topQueries: Map<String, Int>
)

data class SearchInsights(
    val totalSearches: Int,
    val averageResultsPerSearch: Float,
    val searchSuccessRate: Float,
    val averageSearchTime: Long,
    val mostPopularTerms: List<Pair<String, Int>>,
    val topFailedQueries: List<Pair<String, Int>>,
    val searchTrends: List<SearchTrend>,
    val queryPatternInsights: QueryPatternInsights,
    val recommendations: List<String>
)

data class QueryPatternInsights(
    val totalPatterns: Int,
    val successfulPatterns: Int,
    val averageSuccessRate: Float,
    val intentDistribution: Map<SemanticIntent, Float>,
    val typeDistribution: Map<SearchType, Float>,
    val mostSuccessfulIntent: SemanticIntent,
    val mostUsedType: SearchType
)

data class SearchRecommendation(
    val type: RecommendationType,
    val title: String,
    val description: String,
    val suggestions: List<String>
)

enum class RecommendationType {
    SUCCESSFUL_PATTERN, SEARCH_IMPROVEMENT, EXPLORE_FEATURES, SAVE_PRESET
}