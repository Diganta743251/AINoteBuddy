package com.ainotebuddy.app.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val context: Context) : ViewModel() {
    private val noteRepository = NoteRepository(context)
    
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    
    val allNotes: StateFlow<List<NoteEntity>> = noteRepository.getAllNotes()
        .catch { exception ->
            _uiState.value = _uiState.value.copy(error = "Failed to load notes: ${exception.message}")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val favoriteNotes: StateFlow<List<NoteEntity>> = noteRepository.getFavoriteNotes()
        .catch { exception ->
            _uiState.value = _uiState.value.copy(error = "Failed to load favorite notes: ${exception.message}")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    val pinnedNotes: StateFlow<List<NoteEntity>> = noteRepository.getPinnedNotes()
        .catch { exception ->
            _uiState.value = _uiState.value.copy(error = "Failed to load pinned notes: ${exception.message}")
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    
    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            try {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    noteRepository.markAsPinned(noteId, !note.isPinned)
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to toggle pin: ${exception.message}")
            }
        }
    }
    
    fun toggleFavorite(noteId: Long) {
        viewModelScope.launch {
            try {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    noteRepository.markAsFavorite(noteId, !note.isFavorite)
                }
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to toggle favorite: ${exception.message}")
            }
        }
    }
    
    fun deleteNote(noteId: Long) {
        viewModelScope.launch {
            try {
                noteRepository.deleteNoteById(noteId)
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to delete note: ${exception.message}")
            }
        }
    }
    
    fun searchNotes(query: String): StateFlow<List<NoteEntity>> {
        return noteRepository.getSearchResults(query)
            .catch { exception ->
                _uiState.value = _uiState.value.copy(error = "Search failed: ${exception.message}")
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
}

data class MainUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)