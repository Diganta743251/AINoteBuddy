package com.ainotebuddy.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.repository.NoteRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val noteRepository: NoteRepository
) : ViewModel() {
    
    private val _searchResults = MutableStateFlow<List<NoteEntity>>(emptyList())
    val searchResults: StateFlow<List<NoteEntity>> = _searchResults.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private var searchJob: Job? = null
    
    fun search(query: String) {
        // Cancel previous search
        searchJob?.cancel()
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            try {
                // Add a small delay to avoid too many rapid searches
                delay(300)
                
                noteRepository.getSearchResults(query).collect { results ->
                    _searchResults.value = results
                }
            } catch (exception: Exception) {
                // Handle error silently or show error state
                _searchResults.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun clearSearch() {
        searchJob?.cancel()
        _searchResults.value = emptyList()
        _isLoading.value = false
    }
    
    fun togglePin(noteId: Long) {
        viewModelScope.launch {
            try {
                val note = noteRepository.getNoteById(noteId)
                if (note != null) {
                    noteRepository.markAsPinned(noteId, !note.isPinned)
                }
            } catch (exception: Exception) {
                // Handle error silently
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
                // Handle error silently
            }
        }
    }
    
    fun searchByCategory(category: String): StateFlow<List<NoteEntity>> {
        return noteRepository.getNotesByCategory(category)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    fun getFavoriteNotes(): StateFlow<List<NoteEntity>> {
        return noteRepository.getFavoriteNotes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
    
    fun getPinnedNotes(): StateFlow<List<NoteEntity>> {
        return noteRepository.getPinnedNotes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )
    }
}