package com.ainotebuddy.app.ui.viewmodel.template

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.repository.OrganizationRepository
import com.ainotebuddy.app.ui.screens.template.TemplateSortOption
import kotlin.math.max
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the TemplateListScreen
 */
data class TemplateListUiState(
    val isLoading: Boolean = false,
    val templates: List<NoteTemplate> = emptyList(),
    val filteredTemplates: List<NoteTemplate> = emptyList(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedTags: Set<String> = emptySet(),
    val availableTags: List<String> = emptyList(),
    val sortOption: TemplateSortOption = TemplateSortOption.NAME_ASC,
    val errorMessage: String? = null
)

/**
 * ViewModel for the TemplateListScreen
 */
@HiltViewModel
class TemplateListViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TemplateListUiState(isLoading = true))
    val uiState: StateFlow<TemplateListUiState> = _uiState.asStateFlow()
    
    private var searchJob: Job? = null
    private var filterJob: Job? = null
    private var templatesJob: Job? = null
    
    init {
        loadTemplates()
        loadAvailableTags()
    }
    
    /**
     * Load all templates from the repository
     */
    private fun loadTemplates() {
        templatesJob?.cancel()
        templatesJob = viewModelScope.launch {
            organizationRepository.observeTemplates()
                .onStart { 
                    _uiState.update { it.copy(isLoading = true) } 
                }
                .catch { e ->
                    _uiState.update { 
                        it.copy(
                            isLoading = false, 
                            errorMessage = e.message ?: "Failed to load templates"
                        ) 
                    }
                }
                .collect { templates ->
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            templates = templates,
                            filteredTemplates = applyFiltersAndSorting(
                                templates = templates,
                                query = state.searchQuery,
                                tags = state.selectedTags,
                                sortOption = state.sortOption
                            )
                        )
                    }
                }
        }
    }
    
    /**
     * Load all available tags from the repository
     */
    private fun loadAvailableTags() {
        // Derive tags from current templates stream to avoid missing repository API
        viewModelScope.launch {
            organizationRepository.observeTemplates()
                .map { list ->
                    list.flatMap { tpl ->
                        // Support both CSV string and list-type tags if present until model unifies
                        val tagsField = try { tpl::class.java.getDeclaredField("tags").let { f -> f.isAccessible = true; f.get(tpl) } } catch (_: Throwable) { null }
                        when (tagsField) {
                            is List<*> -> tagsField.filterIsInstance<String>()
                            is String -> tagsField.split(',').map { it.trim() }.filter { it.isNotEmpty() }
                            else -> emptyList()
                        }
                    }.distinct().sorted()
                }
                .catch { e ->
                    _uiState.update { it.copy(errorMessage = "Failed to load tags: ${e.message}") }
                }
                .collect { tags ->
                    _uiState.update { it.copy(availableTags = tags) }
                }
        }
    }
    
    /**
     * Handle search query changes
     */
    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        
        // Debounce search
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _uiState.value.let { state ->
                _uiState.update {
                    it.copy(
                        filteredTemplates = applyFiltersAndSorting(
                            templates = state.templates,
                            query = query,
                            tags = state.selectedTags,
                            sortOption = state.sortOption
                        )
                    )
                }
            }
        }
    }
    
    /**
     * Toggle search active state
     */
    fun onSearchActiveChange(isActive: Boolean) {
        _uiState.update { it.copy(isSearchActive = isActive) }
        if (!isActive) {
            onSearchQueryChange("")
        }
    }
    
    /**
     * Handle tag filter changes
     */
    fun onTagFilterChange(tags: Set<String>) {
        _uiState.update { state ->
            state.copy(
                selectedTags = tags,
                filteredTemplates = applyFiltersAndSorting(
                    templates = state.templates,
                    query = state.searchQuery,
                    tags = tags,
                    sortOption = state.sortOption
                )
            )
        }
    }

    /**
     * Toggle favorite (no-op placeholder; underlying model may not support it yet)
     */
    fun onFavoriteToggle(templateId: String) {
        // If future NoteTemplate adds isFavorite, implement repository update here.
    }
    
    /**
     * Toggle a tag in the filter
     */
    fun onTagToggled(tag: String) {
        _uiState.update { state ->
            val newTags = if (state.selectedTags.contains(tag)) {
                state.selectedTags - tag
            } else {
                state.selectedTags + tag
            }
            
            state.copy(
                selectedTags = newTags,
                filteredTemplates = applyFiltersAndSorting(
                    templates = state.templates,
                    query = state.searchQuery,
                    tags = newTags,
                    sortOption = state.sortOption
                )
            )
        }
    }
    
    /**
     * Clear all active filters
     */
    fun onClearFilters() {
        _uiState.update { state ->
            state.copy(
                selectedTags = emptySet(),
                filteredTemplates = applyFiltersAndSorting(
                    templates = state.templates,
                    query = state.searchQuery,
                    tags = emptySet(),
                    sortOption = state.sortOption
                )
            )
        }
    }
    
    /**
     * Handle sort option selection
     */
    fun onSortOptionSelected(sortOption: TemplateSortOption) {
        _uiState.update { state ->
            state.copy(
                sortOption = sortOption,
                filteredTemplates = applyFiltersAndSorting(
                    templates = state.templates,
                    query = state.searchQuery,
                    tags = state.selectedTags,
                    sortOption = sortOption
                )
            )
        }
    }
    
    /**
     * Toggle favorite status for a template
     */
    fun onFavoriteToggle(templateId: String, isFavorite: Boolean) {
        // Minimal placeholder: update local state only if repository API is missing
        viewModelScope.launch {
            try {
                _uiState.update { state ->
                    val updated = state.templates.map { tpl ->
                        if (tpl.id.toString() == templateId) {
                            // Only update if the model supports it
                            try {
                                val copyMethod = tpl::class.members.firstOrNull { it.name == "copy" }
                                val params = copyMethod?.parameters
                                    ?.filter { it.name == null || it.name == "isFavorite" }
                                    ?.associateWith { p -> if (p.name == null) tpl else isFavorite }
                                if (copyMethod != null && params != null) copyMethod.callBy(params) as NoteTemplate else tpl
                            } catch (_: Throwable) { tpl }
                        } else tpl
                    }
                    state.copy(
                        templates = updated,
                        filteredTemplates = applyFiltersAndSorting(
                            templates = updated,
                            query = state.searchQuery,
                            tags = state.selectedTags,
                            sortOption = state.sortOption
                        )
                    )
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(errorMessage = "Failed to update favorite status: ${e.message}") 
                }
            }
        }
    }
    
    /**
     * Clear the current error message
     */
    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }
    
    /**
     * Apply all filters and sorting to the templates list
     */
    private fun applyFiltersAndSorting(
        templates: List<NoteTemplate>,
        query: String,
        tags: Set<String>,
        sortOption: TemplateSortOption
    ): List<NoteTemplate> {
        return templates
            .filter { template ->
                // Apply search query filter
                val matchesQuery = query.isEmpty() ||
                        template.name.contains(query, ignoreCase = true) ||
                        template.description.contains(query, ignoreCase = true) ||
                        template.content.contains(query, ignoreCase = true)
                
                // Apply tag filter
                val templateTags: List<String> = emptyList() // NoteTemplate has no tags field; keep empty for now
                val matchesTags = tags.isEmpty() || tags.all { tag -> templateTags.any { it.equals(tag, ignoreCase = true) } }
                
                matchesQuery && matchesTags
            }
            .sortedWith(
                when (sortOption) {
                    TemplateSortOption.NAME_ASC -> compareBy(String.CASE_INSENSITIVE_ORDER) { it.name }
                    TemplateSortOption.NAME_DESC -> compareByDescending(String.CASE_INSENSITIVE_ORDER) { it.name }
                    TemplateSortOption.LAST_USED -> compareByDescending { it.updatedAt }
                    TemplateSortOption.CREATION_DATE -> compareByDescending { it.createdAt }
                }
            )
    }
    
    override fun onCleared() {
        super.onCleared()
        searchJob?.cancel()
        filterJob?.cancel()
        templatesJob?.cancel()
    }
}
