package com.ainotebuddy.app.ui.viewmodel.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.repository.OrganizationRepository
import com.ainotebuddy.app.data.ai.AISmartOrganizationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing Note Templates
 */
@HiltViewModel
class TemplatesViewModel @Inject constructor(
    private val repository: OrganizationRepository,
    private val aiSmartOrganizationService: AISmartOrganizationService
) : ViewModel() {

    private val _uiState = MutableStateFlow<TemplatesUiState>(TemplatesUiState.Loading)
    val uiState: StateFlow<TemplatesUiState> = _uiState.asStateFlow()

    private val _selectedTemplates = MutableStateFlow<Set<String>>(emptySet())
    val selectedTemplates: StateFlow<Set<String>> = _selectedTemplates.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    private val _categories = MutableStateFlow<List<String>>(emptyList())
    val categories: StateFlow<List<String>> = _categories.asStateFlow()

    private val _selectedCategory = MutableStateFlow<String?>(null)
    val selectedCategory: StateFlow<String?> = _selectedCategory.asStateFlow()

    // Editing state
    private val _editingTemplate = MutableStateFlow<NoteTemplate?>(null)
    val editingTemplate: StateFlow<NoteTemplate?> = _editingTemplate.asStateFlow()

    init {
        loadTemplates()
        loadCategories()
    }

    // UI one-shot events
    sealed class UiEvent {
        data class ShowMessage(val message: String, val templateId: String? = null) : UiEvent()
        data class ShowUndoDelete(val message: String, val template: NoteTemplate) : UiEvent()
    }
    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    // Expose raw templates flow for screens that want direct models
    val templates: StateFlow<List<NoteTemplate>> = repository
        .observeTemplates()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /**
     * Load all templates into UI state
     */
    private fun loadTemplates() {
        viewModelScope.launch {
            repository.observeTemplates()
                .map { templates: List<NoteTemplate> ->
                    templates.map { it.toUiModel() }
                }
                .collect { uiModels: List<TemplateUiModel> ->
                    _uiState.value = TemplatesUiState.Success(uiModels)
                }
        }
    }

    /**
     * Load all template categories
     */
    private fun loadCategories() {
        viewModelScope.launch {
            val allTemplates = repository.getTemplates()
            val categories = allTemplates.map { it.category }.distinct().sorted()
            _categories.value = categories
        }
    }

    /**
     * Filter templates by category
     */
    fun filterByCategory(category: String?) {
        _selectedCategory.value = category
        viewModelScope.launch {
            try {
                val flow = if (category != null) {
                    repository.observeTemplates().map { list -> list.filter { it.category.equals(category, ignoreCase = true) } }
                } else {
                    repository.observeTemplates()
                }
                flow
                    .map { list -> list.map { it.toUiModel() } }
                    .collect { uiModels -> _uiState.value = TemplatesUiState.Success(uiModels) }
            } catch (e: Exception) {
                _uiState.value = TemplatesUiState.Error(e.message ?: "Failed to load templates")
            }
        }
    }

    /**
     * Toggle template selection
     */
    fun toggleTemplateSelection(templateId: String) {
        _selectedTemplates.update { current ->
            if (current.contains(templateId)) {
                current - templateId
            } else {
                current + templateId
            }
        }
    }

    /**
     * Create a new template
     */
    fun createTemplate(
        name: String,
        description: String,
        icon: String,
        category: String,
        content: String,
        variables: List<NoteTemplate.TemplateVariable>
    ) {
        viewModelScope.launch {
            val template = NoteTemplate(
                name = name,
                description = description,
                icon = icon,
                category = category,
                content = content,
                variables = variables
            )
            
            val result: Result<NoteTemplate> = repository.createTemplate(template)
            result.fold(
                onSuccess = {
                    _showCreateDialog.value = false
                    loadTemplates()
                    loadCategories()
                    _events.emit(UiEvent.ShowMessage("Template created"))
                },
                onFailure = { e ->
                    _uiState.value = TemplatesUiState.Error(e.message ?: "Failed to create template")
                    _events.emit(UiEvent.ShowMessage("Failed to create template"))
                }
            )
        }
    }

    /**
     * Add a template directly
     */
    fun addTemplate(template: NoteTemplate) {
        viewModelScope.launch {
            runCatching { repository.createTemplate(template) }
                .onSuccess {
                    loadTemplates()
                    loadCategories()
                    _events.emit(UiEvent.ShowMessage("Template created"))
                }
                .onFailure { e ->
                    _uiState.value = TemplatesUiState.Error(e.message ?: "Failed to add template")
                    _events.emit(UiEvent.ShowMessage("Failed to create template"))
                }
        }
    }

    /**
     * Update a template
     */
    fun updateTemplate(template: NoteTemplate) {
        viewModelScope.launch {
            runCatching { repository.updateTemplate(template) }
                .onSuccess {
                    loadTemplates()
                    loadCategories()
                }
                .onFailure { e ->
                    _uiState.value = TemplatesUiState.Error(e.message ?: "Failed to update template")
                    _events.emit(UiEvent.ShowMessage("Failed to edit template", templateId = template.id))
                }
        }
    }

    /**
     * Delete a template
     */
    fun deleteTemplate(templateId: String) {
        viewModelScope.launch {
            val toDelete = templates.value.find { it.id == templateId }
            runCatching { repository.deleteTemplate(templateId) }
                .onSuccess {
                    _selectedTemplates.value = _selectedTemplates.value - templateId
                    loadTemplates()
                    loadCategories()
                    if (toDelete != null) {
                        _events.emit(UiEvent.ShowUndoDelete("Deleted successfully", toDelete))
                    } else {
                        _events.emit(UiEvent.ShowMessage("Deleted successfully"))
                    }
                }
                .onFailure { e ->
                    _uiState.value = TemplatesUiState.Error(e.message ?: "Failed to delete template")
                    _events.emit(UiEvent.ShowMessage("Failed to delete template"))
                }
        }
    }

    /**
     * Delete selected templates
     */
    fun deleteSelectedTemplates() {
        viewModelScope.launch {
            val templatesToDelete = _selectedTemplates.value
            templatesToDelete.forEach { templateId ->
                repository.deleteTemplate(templateId).onFailure { e ->
                    _uiState.value = TemplatesUiState.Error(e.message ?: "Failed to delete template")
                }
            }
            _selectedTemplates.value = emptySet()
            // Refresh categories in case we deleted the last template in a category
            loadCategories()
        }
    }

    // Start editing a template by ID
    fun startEditTemplate(templateId: String) {
        val current = templates.value.find { it.id == templateId }
        _editingTemplate.value = current
        _showCreateDialog.value = false
    }

    // Save an updated template and close editor
    fun saveEditedTemplate(updated: NoteTemplate) {
        viewModelScope.launch {
            try {
                runCatching { repository.updateTemplate(updated) }
                    .onSuccess {
                        loadTemplates()
                        loadCategories()
                        _editingTemplate.value = null
                        _events.emit(UiEvent.ShowMessage("Edited successfully", templateId = updated.id))
                    }
                    .onFailure {
                        _events.emit(UiEvent.ShowMessage("Failed to edit template", templateId = updated.id))
                    }
            } catch (e: Exception) {
                _events.emit(UiEvent.ShowMessage("Failed to edit template", templateId = updated.id))
            }
        }
    }

    // Dismiss the editor dialog
    fun dismissEditor() {
        _editingTemplate.value = null
    }

    /**
     * Show/hide create template dialog
     */
    fun setShowCreateDialog(show: Boolean) {
        _showCreateDialog.value = show
    }

    /**
     * Generate a template from an existing note
     */
    fun generateTemplateFromNote(noteId: String) {
        viewModelScope.launch {
            _uiState.value = TemplatesUiState.Loading
            val result: Result<NoteTemplate> = aiSmartOrganizationService.generateTemplateFromNote(noteId)
            result.fold(
                onSuccess = { template ->
                    _uiState.value = TemplatesUiState.TemplateGenerated(template)
                },
                onFailure = { e ->
                    _uiState.value = TemplatesUiState.Error(e.message ?: "Failed to generate template")
                }
            )
        }
    }

    /**
     * UI State for Templates screen
     */
    sealed class TemplatesUiState {
        object Loading : TemplatesUiState()
        data class Success(val templates: List<TemplateUiModel>) : TemplatesUiState()
        data class TemplateGenerated(val template: NoteTemplate) : TemplatesUiState()
        data class Error(val message: String) : TemplatesUiState()
    }

    /**
     * UI Model for Template
     */
    data class TemplateUiModel(
        val id: String,
        val name: String,
        val description: String,
        val icon: String,
        val category: String,
        val isSelected: Boolean = false,
        val isDefault: Boolean = false
    )
}

/**
 * Extension function to convert domain model to UI model
 */
private fun NoteTemplate.toUiModel(isSelected: Boolean = false): TemplatesViewModel.TemplateUiModel {
    return TemplatesViewModel.TemplateUiModel(
        id = id,
        name = name,
        description = description,
        icon = icon,
        category = category,
        isSelected = isSelected,
        isDefault = isDefault
    )
}

/**
 * Extension function to convert UI model back to domain model
 */
private fun TemplatesViewModel.TemplateUiModel.toDomainModel(): NoteTemplate {
    return NoteTemplate(
        id = id,
        name = name,
        description = description,
        icon = icon,
        category = category,
        content = "", // This needs to be populated from the full template
        isDefault = isDefault
    )
}
