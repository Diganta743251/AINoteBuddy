package com.ainotebuddy.app.ui.viewmodel.template

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.repository.OrganizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TemplateEditorViewModel @Inject constructor(
    private val organizationRepository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TemplateEditorUiState())
    val uiState: StateFlow<TemplateEditorUiState> = _uiState.asStateFlow()

    fun loadTemplate(templateId: String) {
        viewModelScope.launch {
            // Switch to new OrganizationRepository API as source of truth
            organizationRepository.getTemplateById(templateId)
            // UI state wiring can be extended to reflect loaded data
        }
    }

    fun updateTemplate(template: Any) {
        viewModelScope.launch {
            // Keep placeholder; actual implementation resides in UI/screen ViewModel
        }
    }

    fun saveTemplate() {
        viewModelScope.launch {
            // Delegate to OrganizationRepository when wiring is finalized in screen logic
        }
    }
}

data class TemplateEditorUiState(
    val isLoading: Boolean = false,
    val template: Any? = null,
    val error: String? = null
)