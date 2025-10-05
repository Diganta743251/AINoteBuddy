package com.ainotebuddy.app.ui.viewmodel.organization

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.organization.SmartFolder
import com.ainotebuddy.app.data.repository.OrganizationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing Smart Folders
 */
@HiltViewModel
class SmartFoldersViewModel @Inject constructor(
    private val repository: OrganizationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SmartFoldersUiState>(SmartFoldersUiState.Loading)
    val uiState: StateFlow<SmartFoldersUiState> = _uiState.asStateFlow()

    private val _selectedFolders = MutableStateFlow<Set<String>>(emptySet())
    val selectedFolders: StateFlow<Set<String>> = _selectedFolders.asStateFlow()

    private val _showCreateDialog = MutableStateFlow(false)
    val showCreateDialog: StateFlow<Boolean> = _showCreateDialog.asStateFlow()

    init {
        loadSmartFolders()
    }

    /**
     * Load all smart folders
     */
    private fun loadSmartFolders() {
        viewModelScope.launch {
            repository.observeSmartFolders()
                .map { folders ->
                    folders.map { it.toUiModel() }
                }
                .collect { uiModels ->
                    _uiState.value = SmartFoldersUiState.Success(uiModels)
                }
        }
    }

    /**
     * Toggle folder selection
     */
    fun toggleFolderSelection(folderId: String) {
        _selectedFolders.update { current ->
            if (current.contains(folderId)) {
                current - folderId
            } else {
                current + folderId
            }
        }
    }

    /**
     * Create a new smart folder
     */
    fun createSmartFolder(name: String, description: String, icon: String, color: Int) {
        viewModelScope.launch {
            val folder = SmartFolder(
                name = name,
                description = description,
                icon = icon,
                color = color
            )
            
            val result: Result<SmartFolder> = repository.createSmartFolder(folder)
            result.fold(
                onSuccess = {
                    _showCreateDialog.value = false
                    loadSmartFolders()
                },
                onFailure = { e ->
                    _uiState.value = SmartFoldersUiState.Error(e.message ?: "Failed to create folder")
                }
            )
        }
    }

    /**
     * Delete selected folders
     */
    fun deleteSelectedFolders() {
        viewModelScope.launch {
            val foldersToDelete = _selectedFolders.value
            foldersToDelete.forEach { folderId ->
                repository.deleteSmartFolder(folderId)
            }
            _selectedFolders.value = emptySet()
        }
    }

    /**
     * Show/hide create folder dialog
     */
    fun setShowCreateDialog(show: Boolean) {
        _showCreateDialog.value = show
    }

    /**
     * UI State for Smart Folders screen
     */
    sealed class SmartFoldersUiState {
        object Loading : SmartFoldersUiState()
        data class Success(val folders: List<SmartFolderUiModel>) : SmartFoldersUiState()
        data class Error(val message: String) : SmartFoldersUiState()
    }

    /**
     * UI Model for Smart Folder
     */
    data class SmartFolderUiModel(
        val id: String,
        val name: String,
        val description: String,
        val icon: String,
        val color: Int,
        val noteCount: Int,
        val isSelected: Boolean = false,
        val isEnabled: Boolean = true
    )
}

/**
 * Extension function to convert domain model to UI model
 */
private fun SmartFolder.toUiModel(isSelected: Boolean = false): SmartFoldersViewModel.SmartFolderUiModel {
    return SmartFoldersViewModel.SmartFolderUiModel(
        id = id,
        name = name,
        description = description,
        icon = icon,
        color = color,
        noteCount = noteCount,
        isSelected = isSelected,
        isEnabled = isEnabled
    )
}

/**
 * Extension function to convert UI model back to domain model
 */
private fun SmartFoldersViewModel.SmartFolderUiModel.toDomainModel(): SmartFolder {
    return SmartFolder(
        id = id,
        name = name,
        description = description,
        icon = icon,
        color = color,
        isEnabled = isEnabled,
        noteCount = noteCount
    )
}
