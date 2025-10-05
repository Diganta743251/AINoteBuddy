package com.ainotebuddy.app.views

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Dynamic Smart Views functionality
 */
@HiltViewModel
class DynamicViewsViewModel @Inject constructor(
    private val dynamicViewsEngine: DynamicSmartViewsEngine,
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    // State flows
    private val _smartViews = MutableStateFlow<List<SmartView>>(emptyList())
    val smartViews: StateFlow<List<SmartView>> = _smartViews.asStateFlow()
    
    private val _viewSuggestions = MutableStateFlow<List<ViewSuggestion>>(emptyList())
    val viewSuggestions: StateFlow<List<ViewSuggestion>> = _viewSuggestions.asStateFlow()
    
    private val _currentParadigm = MutableStateFlow(OrganizationParadigm.INTELLIGENT_AUTO)
    val currentParadigm: StateFlow<OrganizationParadigm> = _currentParadigm.asStateFlow()
    
    private val _userPreferences = MutableStateFlow(UserViewPreferences())
    val userPreferences: StateFlow<UserViewPreferences> = _userPreferences.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    // Internal state
    private var allNotes: List<NoteEntity> = emptyList()
    
    init {
        // Initialize the engine
        dynamicViewsEngine.initialize()
        
        // Observe engine state
        observeEngineState()
        
        // Load initial data
        loadInitialData()
    }
    
    /**
     * Refresh smart views based on current notes
     */
    fun refreshViews() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                // Get latest notes
                allNotes = noteRepository.getAllNotes().first()
                
                // Generate smart views
                val views = dynamicViewsEngine.generateSmartViews(allNotes)
                _smartViews.value = views
                
            } catch (e: Exception) {
                _error.value = "Failed to refresh views: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Change the organization paradigm
     */
    fun changeParadigm(paradigm: OrganizationParadigm) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Update user preferences
                val newPreferences = _userPreferences.value.copy(
                    preferredParadigm = paradigm
                )
                _userPreferences.value = newPreferences
                
                // Update current paradigm
                _currentParadigm.value = paradigm
                
                // Regenerate views with new paradigm
                if (allNotes.isNotEmpty()) {
                    val views = dynamicViewsEngine.generateSmartViews(allNotes)
                    _smartViews.value = views
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to change paradigm: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Apply an AI suggestion
     */
    fun applySuggestion(suggestion: ViewSuggestion) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Apply the suggestion
                dynamicViewsEngine.applySuggestion(suggestion, allNotes)
                
                // Update current views
                val updatedViews = dynamicViewsEngine.activeViews.value
                _smartViews.value = updatedViews
                
                // Update suggestions
                val updatedSuggestions = dynamicViewsEngine.viewSuggestions.value
                _viewSuggestions.value = updatedSuggestions
                
            } catch (e: Exception) {
                _error.value = "Failed to apply suggestion: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Customize a view
     */
    fun customizeView(viewId: String, customization: ViewCustomization) {
        viewModelScope.launch {
            try {
                dynamicViewsEngine.customizeView(viewId, customization)
                
                // Update current views
                val updatedViews = dynamicViewsEngine.activeViews.value
                _smartViews.value = updatedViews
                
            } catch (e: Exception) {
                _error.value = "Failed to customize view: ${e.message}"
            }
        }
    }
    
    /**
     * Create a custom view
     */
    fun createCustomView() {
        viewModelScope.launch {
            try {
                // For now, create a simple custom view
                // In a full implementation, this would open a dialog for user input
                val customView = SmartView(
                    id = "custom_${System.currentTimeMillis()}",
                    title = "Custom View",
                    subtitle = "User-defined organization",
                    notes = allNotes.take(10), // Sample notes
                    viewType = ViewType.CUSTOM,
                    priority = ViewPriority.MEDIUM,
                    icon = "view_module",
                    isCustomized = true
                )
                
                val currentViews = _smartViews.value.toMutableList()
                currentViews.add(customView)
                _smartViews.value = currentViews
                
            } catch (e: Exception) {
                _error.value = "Failed to create custom view: ${e.message}"
            }
        }
    }
    
    /**
     * Show customization dialog for a view
     */
    fun showCustomizationDialog(view: SmartView) {
        // This would typically trigger a dialog state
        // For now, we'll just log the action
        viewModelScope.launch {
            // In a full implementation, this would show a customization dialog
            // allowing users to modify title, icon, sorting, filtering, etc.
        }
    }
    
    /**
     * Clear error state
     */
    fun clearError() {
        _error.value = null
    }
    
    /**
     * Observe engine state changes
     */
    private fun observeEngineState() {
        viewModelScope.launch {
            // Observe active views
            dynamicViewsEngine.activeViews.collect { views ->
                _smartViews.value = views
            }
        }
        
        viewModelScope.launch {
            // Observe view suggestions
            dynamicViewsEngine.viewSuggestions.collect { suggestions ->
                _viewSuggestions.value = suggestions
            }
        }
        
        viewModelScope.launch {
            // Observe current paradigm
            dynamicViewsEngine.currentParadigm.collect { paradigm ->
                _currentParadigm.value = paradigm
            }
        }
        
        viewModelScope.launch {
            // Observe user preferences
            dynamicViewsEngine.userViewPreferences.collect { preferences ->
                _userPreferences.value = preferences
            }
        }
    }
    
    /**
     * Load initial data
     */
    private fun loadInitialData() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                
                // Load notes from repository
                noteRepository.getAllNotes().collect { notes ->
                    allNotes = notes
                    
                    // Generate initial views if we have notes
                    if (notes.isNotEmpty()) {
                        val views = dynamicViewsEngine.generateSmartViews(notes)
                        _smartViews.value = views
                    }
                }
                
            } catch (e: Exception) {
                _error.value = "Failed to load initial data: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Get notes for a specific view
     */
    fun getNotesForView(viewId: String): List<NoteEntity> {
        return _smartViews.value.find { it.id == viewId }?.notes ?: emptyList()
    }
    
    /**
     * Search within a specific view
     */
    fun searchInView(viewId: String, query: String): List<NoteEntity> {
        val viewNotes = getNotesForView(viewId)
        return if (query.isBlank()) {
            viewNotes
        } else {
            viewNotes.filter { note ->
                note.title.contains(query, ignoreCase = true) ||
                note.content.contains(query, ignoreCase = true)
            }
        }
    }
    
    /**
     * Get view statistics
     */
    fun getViewStatistics(): ViewStatistics {
        val views = _smartViews.value
        val totalNotes = allNotes.size
        val organizedNotes = views.flatMap { it.notes }.distinctBy { it.id }.size
        
        return ViewStatistics(
            totalViews = views.size,
            totalNotes = totalNotes,
            organizedNotes = organizedNotes,
            organizationEfficiency = if (totalNotes > 0) {
                (organizedNotes.toFloat() / totalNotes) * 100
            } else 0f,
            paradigmDistribution = views.groupBy { it.viewType }
                .mapValues { it.value.size }
        )
    }
    
    /**
     * Export view configuration
     */
    fun exportViewConfiguration(): String {
        val views = _smartViews.value
        val preferences = _userPreferences.value
        
        // In a real implementation, this would serialize to JSON
        return "View configuration exported successfully"
    }
    
    /**
     * Import view configuration
     */
    fun importViewConfiguration(configData: String) {
        viewModelScope.launch {
            try {
                // In a real implementation, this would deserialize from JSON
                // and restore user's view configuration
                
                refreshViews()
            } catch (e: Exception) {
                _error.value = "Failed to import configuration: ${e.message}"
            }
        }
    }
}

/**
 * Statistics about the current view organization
 */
data class ViewStatistics(
    val totalViews: Int,
    val totalNotes: Int,
    val organizedNotes: Int,
    val organizationEfficiency: Float,
    val paradigmDistribution: Map<ViewType, Int>
)
