package com.ainotebuddy.app.ui.screens.template

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.organization.NoteTemplate
import com.ainotebuddy.app.data.repository.OrganizationRepository
// import com.ainotebuddy.app.util.DateTimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

/**
 * UI state for the template editor screen
 */
data class TemplateEditorState(
    val template: NoteTemplate = NoteTemplate(
        id = "",
        name = "",
        description = "",
        icon = "",
        category = "",
        content = "",
        variables = emptyList(),
        isDefault = false,
        isEnabled = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    ),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isSaveSuccess: Boolean = false,
    val isEdited: Boolean = false,
    val error: Throwable? = null
)

/**
 * ViewModel for the template editor screen
 */
@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TemplateEditorState())
    val uiState: StateFlow<TemplateEditorState> = _uiState.asStateFlow()
    
    private var originalTemplate: NoteTemplate? = null
    
    init {
        // Set default template values
        val defaultTemplate = NoteTemplate(
            id = UUID.randomUUID().toString(),
            name = "New Template ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault()).format(java.util.Date())}",
            description = "",
            icon = "📝",
            category = "General",
            content = "# {{title}}\n\nCreated on {{date}}\n\n## Notes\n\n- Item 1\n- Item 2\n\n## Action Items\n\n- [ ] Task 1\n- [ ] Task 2",
            variables = listOf(
                NoteTemplate.TemplateVariable("title", "", "Title"),
                NoteTemplate.TemplateVariable("date", "", "Date", type = NoteTemplate.VariableType.DATE)
            ),
            isDefault = false,
            isEnabled = true
        )
        _uiState.update { it.copy(template = defaultTemplate) }
    }
    
    /**
     * Load a template by ID, or create a new one if ID is null
     */
    fun loadTemplate(templateId: String?) {
        if (templateId.isNullOrBlank()) {
            // New template - already initialized in init
            return
        }
        
        _uiState.update { it.copy(isLoading = true, error = null) }
        
        viewModelScope.launch {
            try {
                val template = organizationRepository.getTemplateById(templateId)
                if (template != null) {
                    originalTemplate = template
                    _uiState.update { 
                        it.copy(
                            template = template,
                            isLoading = false,
                            isEdited = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e,
                        isLoading = false
                    )
                }
            }
        }
    }
    
    /**
     * Update the current template with new values
     */
    fun updateTemplate(template: NoteTemplate) {
        _uiState.update { 
            it.copy(
                template = template,
                isEdited = hasTemplateChanges(template)
            )
        }
    }
    
    /**
     * Save the current template
     * @param asCopy If true, saves as a new template regardless of whether it's new or existing
     */
    fun saveTemplate(asCopy: Boolean = false) {
        val currentState = _uiState.value
        val currentTemplate = currentState.template
        
        // Validate the template
        val validationResult = validateTemplate(currentTemplate)
        if (!validationResult.isValid) {
            _uiState.update {
                it.copy(
                    error = IllegalArgumentException(validationResult.errors.firstOrNull() ?: "Invalid template")
                )
            }
            return
        }
        
        _uiState.update { it.copy(isSaving = true, error = null) }
        
        viewModelScope.launch {
            try {
                val templateToSave = if (asCopy || currentTemplate.id.isBlank()) {
                    // For new templates or when saving as copy, create a new ID
                    currentTemplate.copy(
                        id = UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                } else {
                    // For existing templates, update the timestamp
                    currentTemplate.copy(
                        updatedAt = System.currentTimeMillis()
                    )
                }
                
                if (originalTemplate == null || asCopy) {
                    organizationRepository.createTemplate(templateToSave)
                } else {
                    organizationRepository.updateTemplate(templateToSave)
                }
                
                _uiState.update { 
                    it.copy(
                        template = templateToSave,
                        isSaving = false,
                        isSaveSuccess = true,
                        isEdited = false
                    )
                }
                
                // Update the original template to the saved version
                originalTemplate = templateToSave
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(
                        error = e,
                        isSaving = false
                    )
                }
            }
        }
    }
    
    /**
     * Validate the template
     */
    data class ValidationResult(
        val isValid: Boolean,
        val errors: List<String> = emptyList()
    )

    private fun hasTemplateChanges(newTemplate: NoteTemplate): Boolean {
        val original = originalTemplate ?: return true
        return original != newTemplate
    }

    private fun validateTemplate(template: NoteTemplate): ValidationResult {
        val errors = mutableListOf<String>()
        if (template.name.isBlank()) errors.add("Name cannot be empty")
        if (template.content.isBlank()) errors.add("Content cannot be empty")
        val duplicate = template.variables.groupBy { it.name }.filter { it.value.size > 1 }.keys
        if (duplicate.isNotEmpty()) errors.add("Duplicate variable names: ${duplicate.joinToString()}")
        template.variables.forEach { v ->
            if (v.name.isBlank()) errors.add("Variable name cannot be empty")
            if (v.type == NoteTemplate.VariableType.CHOICE && v.options.isEmpty()) errors.add("Choice variable '${v.name}' must have options")
        }
        return ValidationResult(errors.isEmpty(), errors)
    }
}

/**
 * UI state for the TemplateEditorScreen
 */
data class TemplateEditorUiState(
    val template: NoteTemplate = NoteTemplate(
        id = "",
        name = "",
        description = "",
        icon = "",
        category = "",
        content = "",
        variables = emptyList(),
        isDefault = false,
        isEnabled = true,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    ),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isValid: Boolean = false,
    val errorMessage: String? = null
)
