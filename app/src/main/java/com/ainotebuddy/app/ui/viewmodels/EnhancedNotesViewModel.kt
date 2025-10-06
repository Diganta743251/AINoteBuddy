package com.ainotebuddy.app.ui.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import com.ainotebuddy.app.ui.screens.NoteFilterType
import com.ainotebuddy.app.ui.screens.NotesListUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EnhancedNotesViewModel @Inject constructor(
    private val repository: NoteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NotesListUiState())
    val uiState: StateFlow<NotesListUiState> = _uiState.asStateFlow()

    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList())
    val notes: StateFlow<List<NoteEntity>> = _notes.asStateFlow()

    init {
        loadNotes()
    }

    private fun loadNotes() {
        viewModelScope.launch {
            repository.getAllNotes().collect { notesList ->
                _notes.value = notesList.sortedWith(
                    compareByDescending<NoteEntity> { it.isPinned }
                        .thenByDescending { it.updatedAt }
                )
            }
        }
    }

    fun onFilterChange(filterType: NoteFilterType) {
        _uiState.value = _uiState.value.copy(
            filterType = filterType,
            isSelectionMode = false,
            selectedNotes = emptySet()
        )
    }

    fun onSelectionModeToggle(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(
            isSelectionMode = enabled,
            selectedNotes = if (enabled) _uiState.value.selectedNotes else emptySet()
        )
    }

    fun onNoteSelectionToggle(note: NoteEntity) {
        val currentSelection = _uiState.value.selectedNotes.toMutableSet()
        
        if (currentSelection.contains(note.id)) {
            currentSelection.remove(note.id)
        } else {
            currentSelection.add(note.id)
        }
        
        _uiState.value = _uiState.value.copy(
            selectedNotes = currentSelection,
            isSelectionMode = currentSelection.isNotEmpty()
        )
    }

    fun onSelectAll() {
        val filteredNotes = getFilteredNotes(_notes.value, _uiState.value.filterType)
        val allIds = filteredNotes.map { it.id }.toSet()
        
        _uiState.value = _uiState.value.copy(
            selectedNotes = allIds,
            isSelectionMode = true
        )
    }

    fun onClearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedNotes = emptySet(),
            isSelectionMode = false
        )
    }

    fun onPinClick(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned))
        }
    }

    fun onFavoriteClick(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isFavorite = !note.isFavorite))
        }
    }

    fun onArchiveClick(note: NoteEntity) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isArchived = !note.isArchived))
        }
    }

    fun onDeleteClick(note: NoteEntity) {
        viewModelScope.launch {
            repository.deleteNote(note)
        }
    }

    fun onBatchPin(notes: List<NoteEntity>) {
        viewModelScope.launch {
            val allPinned = notes.all { it.isPinned }
            notes.forEach { note ->
                repository.updateNote(note.copy(isPinned = !allPinned))
            }
            onClearSelection()
        }
    }

    fun onBatchFavorite(notes: List<NoteEntity>) {
        viewModelScope.launch {
            val allFavorite = notes.all { it.isFavorite }
            notes.forEach { note ->
                repository.updateNote(note.copy(isFavorite = !allFavorite))
            }
            onClearSelection()
        }
    }

    fun onBatchArchive(notes: List<NoteEntity>) {
        viewModelScope.launch {
            val allArchived = notes.all { it.isArchived }
            notes.forEach { note ->
                repository.updateNote(note.copy(isArchived = !allArchived))
            }
            onClearSelection()
        }
    }

    fun onBatchDelete(notes: List<NoteEntity>) {
        viewModelScope.launch {
            notes.forEach { note ->
                repository.deleteNote(note)
            }
            onClearSelection()
        }
    }

    private fun getFilteredNotes(notes: List<NoteEntity>, filterType: NoteFilterType): List<NoteEntity> {
        return when (filterType) {
            NoteFilterType.ALL -> notes
            NoteFilterType.PINNED -> notes.filter { it.isPinned }
            NoteFilterType.FAVORITES -> notes.filter { it.isFavorite }
            NoteFilterType.VAULT -> notes.filter { it.isArchived }
            NoteFilterType.RECENT -> {
                val dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                notes.filter { it.updatedAt > dayAgo }
            }
        }
    }
}