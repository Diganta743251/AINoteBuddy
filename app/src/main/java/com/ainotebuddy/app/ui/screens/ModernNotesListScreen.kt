package com.ainotebuddy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.data.PreferencesManager
import com.ainotebuddy.app.viewmodel.NoteViewModel
 

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernNotesListScreen(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    onNewNote: () -> Unit,
    windowSizeClass: WindowSizeClass,
    preferencesManager: PreferencesManager,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState(initial = emptyList())
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "Notes", style = MaterialTheme.typography.titleLarge)
            Button(onClick = onNewNote) { Text("New") }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(notes) { note ->
                Card(onClick = { onNoteClick(note) }, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        Text(note.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleMedium)
                        if (note.content.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                note.content.take(140),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

/*

    // UI State from Preferences
    val uiState by preferencesManager.uiState.collectAsStateWithLifecycle()
    
    // Local state that syncs with persisted state
    val selectedNoteIds = remember { mutableStateOf(uiState.value.selectedNoteIds) }
    val expandedStates = remember { mutableStateOf(uiState.value.expandedStates) }
    // Derived states
    val isSelectionMode = remember { derivedStateOf { selectedNoteIds.value.isNotEmpty() } }
    
    // AI suggestions state
    var aiSuggestions by remember { mutableStateOf(emptyList<AISuggestion>()) }
    
    // Filter notes based on search and filter
    val filteredNotes = remember(notes, searchQuery, selectedFilter) {
        notes.filter { note ->
            val matchesSearch = if (searchQuery.isEmpty()) true else {
                note.title.contains(searchQuery, ignoreCase = true) ||
                note.content.contains(searchQuery, ignoreCase = true)
            }
            
            val matchesFilter = when (selectedFilter) {
                NoteFilter.ALL -> true
                NoteFilter.FAVORITES -> note.isFavorite
                NoteFilter.PINNED -> note.isPinned
                NoteFilter.RECENT -> {
                    val dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                    note.modifiedAt > dayAgo
                }
            }
            
            matchesSearch && matchesFilter
        }
    }
    
    // Generate AI suggestions based on notes
    LaunchedEffect(filteredNotes) {
        if (filteredNotes.isNotEmpty()) {
            aiSuggestions = generateAISuggestions(filteredNotes)
        }
    }
    
    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // Modern top app bar with search
        ModernTopAppBar(
            title = "Notes",
            subtitle = "${filteredNotes.size} notes"
        ) {
            // View mode toggle
            IconButton(
                onClick = { 
                    viewMode = if (viewMode == ViewMode.GRID) ViewMode.LIST else ViewMode.GRID
                    preferencesManager.saveViewMode(
                        if (viewMode == ViewMode.GRID) "list" else "grid"
                    )
                }
            ) {
                Icon(
                    imageVector = if (viewMode == ViewMode.GRID) Icons.Default.ViewList else Icons.Default.ViewModule,
                    contentDescription = "Toggle view mode"
                )
            }
            
            // Filter menu
            var showFilterMenu by remember { mutableStateOf(false) }
            IconButton(onClick = { showFilterMenu = true }) {
                Icon(Icons.Default.FilterList, "Filter")
            }
            
            DropdownMenu(
                expanded = showFilterMenu,
                onDismissRequest = { showFilterMenu = false }
            ) {
                NoteFilter.values().forEach { filter ->
                    DropdownMenuItem(
                        text = { Text(filter.displayName) },
                        onClick = {
                            selectedFilter = filter
                            showFilterMenu = false
                        },
                        leadingIcon = {
                            if (selectedFilter == filter) {
                                Icon(Icons.Default.Check, null)
                            }
                        }
                    )
                }
            }
        }
        
        // Floating search bar
        FloatingSearchBar(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            onSearch = { /* Handle search */ },
            active = isSearchActive,
            onActiveChange = { isSearchActive = it },
            placeholder = "Search notes...",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // AI Suggestions
        if (aiSuggestions.isNotEmpty()) {
            AISuggestionChips(
                suggestions = aiSuggestions,
                onSuggestionClick = { suggestion ->
                    // Handle AI suggestion click
                },
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
        
        // Notes content
        // Main content area
        if (filteredNotes.isEmpty()) {
            EmptyNotesState(
                onNewNote = onNewNote,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            NotesContent(
                notes = filteredNotes,
                viewMode = viewMode,
                windowSizeClass = windowSizeClass,
                onNoteClick = { note ->
                    if (isSelectionMode.value) {
                        // Toggle selection
                        selectedNoteIds.value = selectedNoteIds.value.toMutableSet().apply {
                            if (contains(note.id.toString())) remove(note.id.toString())
                            else add(note.id.toString())
                        }
                        preferencesManager.saveSelectedNoteIds(selectedNoteIds.value)
                    } else {
                        onNoteClick(note)
                    }
                },
                onNoteLongClick = { note ->
                    // Enter selection mode and select the note
                    selectedNoteIds.value = setOf(note.id.toString())
                    preferencesManager.saveSelectedNoteIds(selectedNoteIds.value)
                },
                onNotePinClick = { note ->
                    viewModel.togglePin(note.id)
                },
                onNoteFavoriteClick = { note ->
                    viewModel.toggleFavorite(note.id)
                },
                onNoteDeleteClick = { note ->
                    viewModel.deleteNote(note)
                },
                onNoteArchiveClick = { note ->
                    viewModel.toggleArchive(note.id)
                },
                onNoteShareClick = { note ->
                    // Share the note
                },
                selectedNoteIds = selectedNoteIds.value,
                isSelectionMode = isSelectionMode.value,
                onSelectionChange = { noteId, selected ->
                    selectedNoteIds.value = selectedNoteIds.value.toMutableSet().apply {
                        if (selected) add(noteId) else remove(noteId)
                    }
                    preferencesManager.saveSelectedNoteIds(selectedNoteIds.value)
                },
                onExpandedChange = { noteId, expanded ->
                    expandedStates.value = expandedStates.value.toMutableMap().apply {
                        this[noteId] = expanded
                    }
                    preferencesManager.saveExpandedStates(expandedStates.value)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
        
        // Batch operations bar
        BatchOperationsBar(
            selectedCount = selectedNoteIds.value.size,
            onDeleteClick = {
                // Handle batch delete
                val notesToDelete = filteredNotes.filter { note ->
                    selectedNoteIds.value.contains(note.id.toString())
                }
                notesToDelete.forEach { note ->
                    viewModel.deleteNote(note)
                }
                selectedNoteIds.value = emptySet()
                preferencesManager.saveSelectedNoteIds(selectedNoteIds.value)
            },
            onArchiveClick = {
                // Handle batch archive
                val notesToArchive = filteredNotes.filter { note ->
                    selectedNoteIds.value.contains(note.id.toString())
                }
                notesToArchive.forEach { note ->
                    viewModel.toggleArchive(note.id)
                }
                selectedNoteIds.value = emptySet()
                preferencesManager.saveSelectedNoteIds(selectedNoteIds.value)
            },
            onShareClick = {
                // Handle batch share
                // This would typically open the share sheet with all selected notes
            },
            onSelectAllClick = { select ->
                selectedNoteIds.value = if (select) {
                    filteredNotes.map { it.id.toString() }.toSet()
                } else {
                    emptySet()
                }
                preferencesManager.saveSelectedNoteIds(selectedNoteIds.value)
            },
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }
}

 

@Composable
private fun EmptyNotesState(
    onNewNote: () -> Unit,
    modifier: Modifier = Modifier
) {
}

// (NoteFilter, ViewMode removed - not used in simplified screen)

// (NoteEntity.toNoteCardData removed - not used in simplified screen)

// (generateAISuggestions removed - not used in simplified screen)

*/
