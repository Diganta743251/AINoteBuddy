package com.ainotebuddy.app.search

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.Json

// DataStore extension
private val Context.savedSearchDataStore: DataStore<Preferences> by preferencesDataStore(name = "saved_searches")

/**
 * Manager for saved search presets and search history
 */
class SavedSearchManager(private val context: Context) {
    private val json = Json { ignoreUnknownKeys = true }
    
    // DataStore keys
    private val savedSearchesKey = stringPreferencesKey("saved_searches")
    private val searchHistoryKey = stringPreferencesKey("search_history")
    private val searchAnalyticsKey = stringPreferencesKey("search_analytics")
    
    // Flows for reactive UI
    private val _savedSearches = MutableStateFlow<List<SavedSearchPreset>>(emptyList())
    val savedSearches: StateFlow<List<SavedSearchPreset>> = _savedSearches.asStateFlow()
    
    private val _searchHistory = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())
    val searchHistory: StateFlow<List<SearchHistoryEntry>> = _searchHistory.asStateFlow()
    
    init {
        // Load saved data on initialization
        loadSavedSearches()
        loadSearchHistory()
    }
    
    /**
     * Save a search query as a preset
     */
    suspend fun saveSearchPreset(
        name: String,
        description: String,
        query: SmartSearchQuery,
        category: SearchPresetCategory = SearchPresetCategory.PERSONAL
    ): String {
        val preset = SavedSearchPreset(
            id = "search_${System.currentTimeMillis()}",
            name = name,
            description = description,
            query = query,
            category = category,
            createdAt = System.currentTimeMillis()
        )
        
        val currentPresets = _savedSearches.value.toMutableList()
        currentPresets.add(preset)
        
        // Save to DataStore
        context.savedSearchDataStore.edit { preferences ->
            val serializablePresets = currentPresets.map { it.toSerializable() }
            preferences[savedSearchesKey] = json.encodeToString(serializablePresets)
        }
        
        _savedSearches.value = currentPresets
        return preset.id
    }
    
    /**
     * Delete a saved search preset
     */
    suspend fun deleteSavedSearch(presetId: String) {
        val currentPresets = _savedSearches.value.filter { it.id != presetId }
        
        context.savedSearchDataStore.edit { preferences ->
            val serializablePresets = currentPresets.map { it.toSerializable() }
            preferences[savedSearchesKey] = json.encodeToString(serializablePresets)
        }
        
        _savedSearches.value = currentPresets
    }
    
    /**
     * Update usage statistics for a saved search
     */
    suspend fun recordSearchUsage(presetId: String) {
        val currentPresets = _savedSearches.value.toMutableList()
        val index = currentPresets.indexOfFirst { it.id == presetId }
        
        if (index != -1) {
            val preset = currentPresets[index]
            currentPresets[index] = preset.copy(
                usageCount = preset.usageCount + 1,
                lastUsed = System.currentTimeMillis()
            )
            
            // Save updated presets
            context.savedSearchDataStore.edit { preferences ->
                val serializablePresets = currentPresets.map { it.toSerializable() }
                preferences[savedSearchesKey] = json.encodeToString(serializablePresets)
            }
            
            _savedSearches.value = currentPresets
        }
    }
    
    /**
     * Add search query to history
     */
    suspend fun addToSearchHistory(query: String, resultCount: Int) {
        val historyEntry = SearchHistoryEntry(
            query = query,
            timestamp = System.currentTimeMillis(),
            resultCount = resultCount
        )
        
        val currentHistory = _searchHistory.value.toMutableList()
        
        // Remove duplicate if exists
        currentHistory.removeAll { it.query == query }
        
        // Add to beginning
        currentHistory.add(0, historyEntry)
        
        // Keep only last 50 entries
        val trimmedHistory = currentHistory.take(50)
        
        // Save to DataStore
        context.savedSearchDataStore.edit { preferences ->
            val serializableHistory = trimmedHistory.map { it.toSerializable() }
            preferences[searchHistoryKey] = json.encodeToString(serializableHistory)
        }
        
        _searchHistory.value = trimmedHistory
    }
    
    /**
     * Clear search history
     */
    suspend fun clearSearchHistory() {
        context.savedSearchDataStore.edit { preferences ->
            preferences.remove(searchHistoryKey)
        }
        _searchHistory.value = emptyList()
    }
    
    /**
     * Get popular searches based on history
     */
    fun getPopularSearches(limit: Int = 10): List<String> {
        return _searchHistory.value
            .groupBy { it.query }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(limit)
            .map { it.first }
    }
    
    /**
     * Get recent searches
     */
    fun getRecentSearches(limit: Int = 10): List<String> {
        return _searchHistory.value
            .take(limit)
            .map { it.query }
    }
    
    /**
     * Get saved searches by category
     */
    fun getSavedSearchesByCategory(category: SearchPresetCategory): List<SavedSearchPreset> {
        return _savedSearches.value.filter { it.category == category }
    }
    
    /**
     * Get most used saved searches
     */
    fun getMostUsedSavedSearches(limit: Int = 5): List<SavedSearchPreset> {
        return _savedSearches.value
            .sortedByDescending { it.usageCount }
            .take(limit)
    }
    
    /**
     * Get default search presets for different workflows
     */
    fun getDefaultSearchPresets(): List<SavedSearchPreset> {
        return listOf(
            SavedSearchPreset(
                id = "default_recent",
                name = "Recent Notes",
                description = "Notes from the last 7 days",
                query = SmartSearchQuery(
                    rawQuery = "this week",
                    processedQuery = "this week",
                    searchTerms = emptyList(),
                    filters = SearchFilters(
                        dateRange = DateRange(
                            startDate = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000),
                            endDate = System.currentTimeMillis(),
                            relativeType = RelativeDateType.THIS_WEEK
                        )
                    ),
                    semanticIntent = SemanticIntent.FIND_RECENT
                ),
                category = SearchPresetCategory.RECENT,
                isDefault = true
            ),
            SavedSearchPreset(
                id = "default_work",
                name = "Work Notes",
                description = "All work-related notes and meetings",
                query = SmartSearchQuery(
                    rawQuery = "work meeting project",
                    processedQuery = "work meeting project",
                    searchTerms = listOf("work", "meeting", "project"),
                    filters = SearchFilters(
                        categories = listOf("work", "business"),
                        noteTypes = listOf(NoteType.MEETING, NoteType.TEXT)
                    ),
                    semanticIntent = SemanticIntent.FIND_BY_TOPIC
                ),
                category = SearchPresetCategory.WORK,
                isDefault = true
            ),
            SavedSearchPreset(
                id = "default_important",
                name = "Important Notes",
                description = "Pinned and high-priority notes",
                query = SmartSearchQuery(
                    rawQuery = "important pinned",
                    processedQuery = "important pinned",
                    searchTerms = listOf("important", "pinned"),
                    filters = SearchFilters(
                        isPinned = true
                    ),
                    semanticIntent = SemanticIntent.FIND_IMPORTANT
                ),
                category = SearchPresetCategory.FAVORITES,
                isDefault = true
            ),
            SavedSearchPreset(
                id = "default_media",
                name = "Media Notes",
                description = "Notes with images, voice recordings, and drawings",
                query = SmartSearchQuery(
                    rawQuery = "voice image drawing",
                    processedQuery = "voice image drawing",
                    searchTerms = listOf("voice", "image", "drawing"),
                    filters = SearchFilters(
                        noteTypes = listOf(NoteType.VOICE, NoteType.IMAGE, NoteType.DRAWING),
                        hasAttachments = true
                    ),
                    semanticIntent = SemanticIntent.FIND_BY_TYPE
                ),
                category = SearchPresetCategory.PERSONAL,
                isDefault = true
            ),
            SavedSearchPreset(
                id = "default_tasks",
                name = "Tasks & TODOs",
                description = "Unfinished tasks and checklists",
                query = SmartSearchQuery(
                    rawQuery = "todo task checklist unfinished",
                    processedQuery = "todo task checklist unfinished",
                    searchTerms = listOf("todo", "task", "checklist", "unfinished"),
                    filters = SearchFilters(
                        noteTypes = listOf(NoteType.CHECKLIST)
                    ),
                    semanticIntent = SemanticIntent.FIND_UNFINISHED
                ),
                category = SearchPresetCategory.PRODUCTIVITY,
                isDefault = true
            )
        )
    }
    /**
     * Load saved searches from DataStore
     */
    private fun loadSavedSearches() {
        context.savedSearchDataStore.data
            .map { preferences ->
                preferences[savedSearchesKey]?.let { jsonStr ->
                    try {
                        val serializablePresets = json.decodeFromString<List<SerializableSavedSearchPreset>>(jsonStr)
                        serializablePresets.map { it.toSavedSearchPreset() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: getDefaultSearchPresets()
            }
            .onEach { presets ->
                _savedSearches.value = presets
            }
            .launchIn(kotlinx.coroutines.GlobalScope)
    }
    
    /**
     * Load search history from DataStore
     */
    private fun loadSearchHistory() {
        context.savedSearchDataStore.data
            .map { preferences ->
                preferences[searchHistoryKey]?.let { jsonStr ->
                    try {
                        val serializableHistory = json.decodeFromString<List<SerializableSearchHistoryEntry>>(jsonStr)
                        serializableHistory.map { it.toSearchHistoryEntry() }
                    } catch (e: Exception) {
                        emptyList()
                    }
                } ?: emptyList()
            }
            .onEach { history ->
                _searchHistory.value = history
            }
            .launchIn(kotlinx.coroutines.GlobalScope)
    }
}

/**
 * Search history entry
 */
@Serializable
data class SearchHistoryEntry(
    val query: String,
    val timestamp: Long,
    val resultCount: Int
) {
    fun toSerializable() = SerializableSearchHistoryEntry(
        query = query,
        timestamp = timestamp,
        resultCount = resultCount
    )
}

@Serializable
data class SerializableSearchHistoryEntry(
    val query: String,
    val timestamp: Long,
    val resultCount: Int
) {
    fun toSearchHistoryEntry() = SearchHistoryEntry(
        query = query,
        timestamp = timestamp,
        resultCount = resultCount
    )
}

/**
 * Serializable version of SavedSearchPreset
 */
@Serializable
data class SerializableSavedSearchPreset(
    val id: String,
    val name: String,
    val description: String,
    val query: SmartSearchQuery,
    val category: SearchPresetCategory,
    val isDefault: Boolean = false,
    val usageCount: Int = 0,
    val lastUsed: Long = 0L,
    val createdAt: Long
) {
    fun toSavedSearchPreset() = SavedSearchPreset(
        id = id,
        name = name,
        description = description,
        query = query,
        category = category,
        isDefault = isDefault,
        usageCount = usageCount,
        lastUsed = lastUsed,
        createdAt = createdAt
    )
}

/**
 * Extension function to convert SavedSearchPreset to serializable
 */
fun SavedSearchPreset.toSerializable() = SerializableSavedSearchPreset(
    id = id,
    name = name,
    description = description,
    query = query,
    category = category,
    isDefault = isDefault,
    usageCount = usageCount,
    lastUsed = lastUsed,
    createdAt = createdAt
)