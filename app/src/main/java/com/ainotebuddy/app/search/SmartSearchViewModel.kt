package com.ainotebuddy.app.search

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.Note
import com.ainotebuddy.app.viewmodel.NoteViewModel
import com.ainotebuddy.app.data.toDomain
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for Smart Search functionality with comprehensive search management
 */
class SmartSearchViewModel(
    application: Application,
    private val noteViewModel: NoteViewModel
) : AndroidViewModel(application) {
    
    private val context = application.applicationContext
    
    // Core search components
    private val searchEngine = SmartSearchEngine(context)
    private val savedSearchManager = SavedSearchManager(context)
    private val searchAnalyticsManager = SearchAnalyticsManager(context)
    private val searchIndexManager = SearchIndexManager(context)
    private val nlpProcessor = NLPSearchProcessor(context)
    
    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()
    
    private val _searchResults = MutableStateFlow<SearchResults?>(null)
    val searchResults: StateFlow<SearchResults?> = _searchResults.asStateFlow()
    
    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()
    
    private val _searchSuggestions = MutableStateFlow<List<SearchSuggestion>>(emptyList())
    val searchSuggestions: StateFlow<List<SearchSuggestion>> = _searchSuggestions.asStateFlow()
    
    private val _nlpSuggestions = MutableStateFlow<List<String>>(emptyList())
    val nlpSuggestions: StateFlow<List<String>> = _nlpSuggestions.asStateFlow()
    
    private val _showSuggestions = MutableStateFlow(false)
    val showSuggestions: StateFlow<Boolean> = _showSuggestions.asStateFlow()
    
    private val _nlpParameters = MutableStateFlow<SearchParameters?>(null)
    val nlpParameters: StateFlow<SearchParameters?> = _nlpParameters.asStateFlow()
    
    // Search history and analytics
    val savedSearches = savedSearchManager.savedSearches
    val searchHistory = savedSearchManager.searchHistory
    val searchAnalytics = searchAnalyticsManager.analytics
    val indexStats = searchIndexManager.indexStats
    
    // Advanced search state
    private val _selectedFilters = MutableStateFlow<Set<String>>(emptySet())
    val selectedFilters: StateFlow<Set<String>> = _selectedFilters.asStateFlow()
    
    private val _showAdvancedFilters = MutableStateFlow(false)
    val showAdvancedFilters: StateFlow<Boolean> = _showAdvancedFilters.asStateFlow()
    
    // Search insights and recommendations
    private val _searchInsights = MutableStateFlow<SearchInsights?>(null)
    val searchInsights: StateFlow<SearchInsights?> = _searchInsights.asStateFlow()
    
    private val _personalizedRecommendations = MutableStateFlow<List<SearchRecommendation>>(emptyList())
    val personalizedRecommendations: StateFlow<List<SearchRecommendation>> = _personalizedRecommendations.asStateFlow()
    
    init {
        // Initialize search index when notes change
        noteViewModel.notes
            .onEach { notes ->
                if (notes.isNotEmpty()) {
                    // Map Room entities to domain model used by SearchIndexManager
                    val domainNotes = notes.map { it.toDomain() }
                    rebuildSearchIndex(domainNotes)
                }
            }
            .launchIn(viewModelScope)
        
        // Generate search suggestions when query changes
        searchQuery
            .debounce(300)
            .onEach { query ->
                if (query.isNotBlank()) {
                    updateSuggestions(query)
                } else {
                    _searchSuggestions.value = emptyList()
                    _nlpSuggestions.value = emptyList()
                }
            }
            .launchIn(viewModelScope)
        
        // Load search insights periodically
        viewModelScope.launch {
            while (true) {
                delay(60000) // Update every minute
                updateSearchInsights()
            }
        }
    }
    
    /**
     * Perform smart search with natural language processing
     */
    fun performSearch(query: String) {
        _searchQuery.value = query
        _showSuggestions.value = false
        
        if (query.isBlank()) {
            _searchResults.value = null
            _nlpParameters.value = null
            return
        }
        
        viewModelScope.launch {
            _isSearching.value = true
            
            try {
                // Process natural language query
                val params = nlpProcessor.processQuery(query)
                _nlpParameters.value = params
                
                // Perform the search with NLP parameters
                val results = searchEngine.searchWithParams(
                    query = params.query,
                    dateRange = params.dateRange?.let { DateRange(startDate = it.first, endDate = it.second) },
                    tags = params.tags,
                    priority = params.priority,
                    isPinned = params.isPinned
                )
                
                _searchResults.value = results
                
                // Record analytics and history
                searchAnalyticsManager.recordSearch(results.query, results.totalResults, results.searchTime, true)
                savedSearchManager.addToSearchHistory(query, results.totalResults)
                
            } catch (e: Exception) {
                // Fall back to basic search if NLP processing fails
                fallbackToBasicSearch(query)
            } finally {
                _isSearching.value = false
            }
        }
    }
    
    private suspend fun fallbackToBasicSearch(query: String) {
        try {
            val results = searchEngine.search(query)
            _searchResults.value = results
            searchAnalyticsManager.recordSearch(results.query, results.totalResults, results.searchTime, true)
            savedSearchManager.addToSearchHistory(query, results.totalResults)
        } catch (e: Exception) {
            _searchResults.value = SearchResults(
                query = SmartSearchQuery(rawQuery = query, processedQuery = query),
                results = emptyList(),
                totalResults = 0,
                searchTime = 0L,
                fromCache = false,
                suggestions = emptyList()
            )
        }
    }
    
    /**
     * Updates search suggestions based on the current query
     */
    fun updateSuggestions(query: String) {
        _searchQuery.value = query
        
        if (query.length < 2) {
            _searchSuggestions.value = emptyList()
            _nlpSuggestions.value = emptyList()
            _showSuggestions.value = false
            return
        }
        
        viewModelScope.launch {
            try {
                // Get suggestions from search engine
                val searchSuggestions = searchEngine.getSuggestions(query)
                _searchSuggestions.value = searchSuggestions
                
                // Get NLP-based suggestions
                val notesDomain = noteViewModel.notes.value.map { it.toDomain() }
                val nlpSuggestions = nlpProcessor.getSuggestions(query, notesDomain)
                _nlpSuggestions.value = nlpSuggestions
                
                // Show suggestions if we have any
                _showSuggestions.value = searchSuggestions.isNotEmpty() || nlpSuggestions.isNotEmpty()
                
            } catch (e: Exception) {
                _searchSuggestions.value = emptyList()
                _nlpSuggestions.value = emptyList()
                _showSuggestions.value = false
            }
        }
    }
    
    /**
     * Apply a search suggestion
     */
    fun applySuggestion(suggestion: SearchSuggestion) {
        _searchQuery.value = suggestion.suggestion
        _showSuggestions.value = false
        performSearch(suggestion.suggestion)
    }
    
    /**
     * Apply a saved search preset
     */
    fun applySavedSearch(preset: SavedSearchPreset) {
        viewModelScope.launch {
            _searchQuery.value = preset.query.rawQuery
            performSearch(preset.query.rawQuery)
            savedSearchManager.recordSearchUsage(preset.id)
        }
    }
    
    /**
     * Save current search as a preset
     */
    fun saveCurrentSearch(name: String, description: String, category: SearchPresetCategory) {
        viewModelScope.launch {
            val currentQuery = _searchQuery.value
            if (currentQuery.isNotBlank()) {
                val smartQuery = SmartSearchQuery(rawQuery = currentQuery)
                savedSearchManager.saveSearchPreset(name, description, smartQuery, category)
            }
        }
    }
    
    /**
     * Delete a saved search preset
     */
    fun deleteSavedSearch(presetId: String) {
        viewModelScope.launch {
            savedSearchManager.deleteSavedSearch(presetId)
        }
    }
    
    /**
     * Toggle advanced filters visibility
     */
    fun toggleAdvancedFilters() {
        _showAdvancedFilters.value = !_showAdvancedFilters.value
    }
    
    /**
     * Update selected filters
     */
    fun updateFilters(filters: Set<String>) {
        _selectedFilters.value = filters
        
        // Re-run search with new filters if there's an active query
        val currentQuery = _searchQuery.value
        if (currentQuery.isNotBlank()) {
            performSearch(currentQuery)
        }
    }
    
    /**
     * Clear search and reset state
     */
    fun clearSearch() {
        _searchQuery.value = ""
        _searchResults.value = null
        _searchSuggestions.value = emptyList()
        _nlpSuggestions.value = emptyList()
        _showSuggestions.value = false
        _selectedFilters.value = emptySet()
        _showAdvancedFilters.value = false
    }
    
    /**
     * Get search suggestions for auto-completion
     */
    private suspend fun generateSearchSuggestions(query: String) {
        try {
            // Build simple analytics suggestions from recent searches
            val analyticsSuggestions = savedSearchManager.getRecentSearches()
                .filter { it.contains(query, ignoreCase = true) }
                .map { recent ->
                    SearchSuggestion(
                        suggestion = recent,
                        type = SuggestionType.RECENT_SEARCH,
                        confidence = 0.6f
                    )
                }
            
            // Get suggestions from search index
            val indexSuggestions = searchIndexManager.getIndexSuggestions(query)
                .map { suggestion ->
                    SearchSuggestion(
                        suggestion = suggestion,
                        type = SuggestionType.QUERY_COMPLETION,
                        confidence = 0.7f
                    )
                }
            
            // Get recent searches that match
            val recentSuggestions = emptyList<SearchSuggestion>()
            
            // Combine and rank suggestions
            val allSuggestions = (analyticsSuggestions + indexSuggestions + recentSuggestions)
                .distinctBy { it.suggestion }
                .sortedByDescending { it.confidence }
                .take(8)
            
            _searchSuggestions.value = allSuggestions
            
        } catch (e: Exception) {
            _searchSuggestions.value = emptyList()
        }
    }
    
    /**
     * Rebuild search index from current notes
     */
    private suspend fun rebuildSearchIndex(notes: List<Note>) {
        try {
            searchIndexManager.buildIndex(notes)
        } catch (e: Exception) {
            // Handle index build error
        }
    }
    
    /**
     * Update search insights and recommendations
     */
    private suspend fun updateSearchInsights() {
        // Minimal placeholder: analytics API not exposed; skip insights for now
        _searchInsights.value = null
        _personalizedRecommendations.value = emptyList()
    }
    
    /**
     * Optimize search performance
     */
    fun optimizeSearch() {
        viewModelScope.launch {
            try {
                searchIndexManager.optimizeIndex()
            } catch (e: Exception) {
                // Handle optimization error
            }
        }
    }
    
    /**
     * Get search health metrics
     */
    fun getSearchHealth(): IndexHealthMetrics {
        return searchIndexManager.getIndexHealth()
    }
    
    /**
     * Export search analytics
     */
    fun exportSearchAnalytics(): SearchInsights? {
        return _searchInsights.value
    }
    
    /**
     * Get popular searches for dashboard widget
     */
    fun getPopularSearches(limit: Int = 5): List<String> {
        return savedSearchManager.getPopularSearches(limit)
    }
    
    /**
     * Get recent searches for dashboard widget
     */
    fun getRecentSearches(limit: Int = 5): List<String> {
        return savedSearchManager.getRecentSearches(limit)
    }
    
    /**
     * Get most used saved searches for dashboard widget
     */
    fun getMostUsedSavedSearches(limit: Int = 5): List<SavedSearchPreset> {
        return savedSearchManager.getMostUsedSavedSearches(limit)
    }
    
    /**
     * Clear search history
     */
    fun clearSearchHistory() {
        viewModelScope.launch {
            savedSearchManager.clearSearchHistory()
        }
    }
    
    /**
     * Get search suggestions for empty state
     */
    fun getExampleQueries(): List<String> {
        return listOf(
            "meeting notes from last week",
            "important tasks #work",
            "voice notes about project",
            "images from yesterday",
            "notes similar to productivity",
            "unfinished todos",
            "notes with reminders",
            "long articles to read"
        )
    }
    
    /**
     * Get search tips for new users
     */
    fun getSearchTips(): List<String> {
        return listOf(
            "meeting notes",
            "today",
            "important",
            "#work",
            "last week",
            "voice notes",
            "with images",
            "untagged"
        )
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up resources if needed
    }
}