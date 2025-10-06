package com.ainotebuddy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.data.NoteEntity
import me.saket.swipe.SwipeAction
import me.saket.swipe.SwipeableActionsBox
import androidx.compose.foundation.BorderStroke

data class NotesListUiState(
    val isSelectionMode: Boolean = false,
    val selectedNotes: Set<Long> = emptySet(),
    val filterType: NoteFilterType = NoteFilterType.ALL
)

enum class NoteFilterType(val displayName: String) {
    ALL("All Notes"),
    PINNED("Pinned"),
    FAVORITES("Favorites"),
    VAULT("Vault"),
    RECENT("Recent")
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedNotesListScreen(
    notes: List<NoteEntity>,
    uiState: NotesListUiState,
    onNoteClick: (NoteEntity) -> Unit,
    onSelectionModeToggle: (Boolean) -> Unit,
    onNoteSelectionToggle: (NoteEntity) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onPinClick: (NoteEntity) -> Unit,
    onFavoriteClick: (NoteEntity) -> Unit,
    onArchiveClick: (NoteEntity) -> Unit,
    onDeleteClick: (NoteEntity) -> Unit,
    onBatchDelete: (List<NoteEntity>) -> Unit,
    onBatchArchive: (List<NoteEntity>) -> Unit,
    onBatchPin: (List<NoteEntity>) -> Unit,
    onFilterChange: (NoteFilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredNotes = remember(notes, uiState.filterType) {
        when (uiState.filterType) {
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
    
    val selectedNotesList = remember(uiState.selectedNotes, filteredNotes) {
        filteredNotes.filter { uiState.selectedNotes.contains(it.id) }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top bar with filter and selection controls
        EnhancedNotesTopBar(
            filterType = uiState.filterType,
            isSelectionMode = uiState.isSelectionMode,
            selectedCount = uiState.selectedNotes.size,
            totalCount = filteredNotes.size,
            onFilterChange = onFilterChange,
            onSelectAll = onSelectAll,
            onClearSelection = onClearSelection,
            onExitSelectionMode = { onSelectionModeToggle(false) }
        )
        
        // Batch operations bar
        AnimatedVisibility(
            visible = uiState.isSelectionMode && uiState.selectedNotes.isNotEmpty(),
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut()
        ) {
            BatchOperationsBar(
                selectedNotes = selectedNotesList,
                onBatchDelete = onBatchDelete,
                onBatchArchive = onBatchArchive,
                onBatchPin = onBatchPin,
                onClearSelection = onClearSelection
            )
        }
        
        // Notes list
        if (filteredNotes.isEmpty()) {
            EmptyNotesState(
                filterType = uiState.filterType,
                modifier = Modifier.weight(1f)
            )
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredNotes, key = { it.id }) { note ->
                    EnhancedNoteCard(
                        note = note,
                        isSelected = uiState.selectedNotes.contains(note.id),
                        isSelectionMode = uiState.isSelectionMode,
                        onClick = { 
                            if (uiState.isSelectionMode) {
                                onNoteSelectionToggle(note)
                            } else {
                                onNoteClick(note)
                            }
                        },
                        onLongClick = {
                            if (!uiState.isSelectionMode) {
                                onSelectionModeToggle(true)
                                onNoteSelectionToggle(note)
                            }
                        },
                        onPinClick = { onPinClick(note) },
                        onFavoriteClick = { onFavoriteClick(note) },
                        onArchiveClick = { onArchiveClick(note) },
                        onDeleteClick = { onDeleteClick(note) },
                        modifier = Modifier
                    )
                }
            }
        }
    }
}

@Composable
fun EnhancedNotesTopBar(
    filterType: NoteFilterType,
    isSelectionMode: Boolean,
    selectedCount: Int,
    totalCount: Int,
    onFilterChange: (NoteFilterType) -> Unit,
    onSelectAll: () -> Unit,
    onClearSelection: () -> Unit,
    onExitSelectionMode: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelectionMode) 4.dp else 1.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isSelectionMode) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onExitSelectionMode) {
                            Icon(Icons.Default.Close, contentDescription = "Exit selection")
                        }
                        Text(
                            text = "$selectedCount selected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Row {
                        TextButton(
                            onClick = if (selectedCount == totalCount) onClearSelection else onSelectAll
                        ) {
                            Text(if (selectedCount == totalCount) "Deselect All" else "Select All")
                        }
                    }
                } else {
                    Text(
                        text = "${totalCount} ${if (totalCount == 1) "note" else "notes"}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    // Filter dropdown
                    var showFilterMenu by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            onClick = { showFilterMenu = true },
                            label = { Text(filterType.displayName) },
                            selected = filterType != NoteFilterType.ALL,
                            trailingIcon = {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Filter options"
                                )
                            }
                        )
                        
                        DropdownMenu(
                            expanded = showFilterMenu,
                            onDismissRequest = { showFilterMenu = false }
                        ) {
                            NoteFilterType.values().forEach { filter ->
                                DropdownMenuItem(
                                    text = { Text(filter.displayName) },
                                    onClick = {
                                        onFilterChange(filter)
                                        showFilterMenu = false
                                    },
                                    leadingIcon = {
                                        if (filterType == filter) {
                                            Icon(Icons.Default.Check, contentDescription = null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BatchOperationsBar(
    selectedNotes: List<NoteEntity>,
    onBatchDelete: (List<NoteEntity>) -> Unit,
    onBatchArchive: (List<NoteEntity>) -> Unit,
    onBatchPin: (List<NoteEntity>) -> Unit,
    onClearSelection: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Pin/Unpin
            val allPinned = selectedNotes.all { it.isPinned }
            OutlinedButton(
                onClick = { onBatchPin(selectedNotes) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (allPinned) Icons.Default.PushPin else Icons.Default.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (allPinned) "Unpin" else "Pin")
            }
            
            // Archive/Unarchive
            val allArchived = selectedNotes.all { it.isArchived }
            OutlinedButton(
                onClick = { onBatchArchive(selectedNotes) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    if (allArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (allArchived) "Restore" else "Archive")
            }
            
            // Delete
            OutlinedButton(
                onClick = { onBatchDelete(selectedNotes) },
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Delete")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EnhancedNoteCard(
    note: NoteEntity,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onPinClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pinAction = SwipeAction(
        onSwipe = { onPinClick() },
        icon = { 
            Icon(
                if (note.isPinned) Icons.Default.PushPin else Icons.Default.Star,
                contentDescription = if (note.isPinned) "Unpin" else "Pin",
                tint = Color.White
            )
        },
        background = Color(0xFFFFD700),
        isUndo = true
    )
    
    val favoriteAction = SwipeAction(
        onSwipe = { onFavoriteClick() },
        icon = { 
            Icon(
                if (note.isFavorite) Icons.Default.FavoriteBorder else Icons.Default.Favorite,
                contentDescription = if (note.isFavorite) "Unfavorite" else "Favorite",
                tint = Color.White
            )
        },
        background = Color(0xFFFC5C7D),
        isUndo = true
    )
    
    val archiveAction = SwipeAction(
        onSwipe = { onArchiveClick() },
        icon = { 
            Icon(
                if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                contentDescription = if (note.isArchived) "Restore" else "Archive",
                tint = Color.White
            )
        },
        background = Color(0xFF00BCD4),
        isUndo = true
    )
    
    val deleteAction = SwipeAction(
        onSwipe = { onDeleteClick() },
        icon = { 
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White
            )
        },
        background = MaterialTheme.colorScheme.error,
        isUndo = true
    )
    
    SwipeableActionsBox(
        startActions = listOf(pinAction, favoriteAction),
        endActions = listOf(archiveAction, deleteAction),
        modifier = modifier
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isSelected) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else 
                    MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(16.dp),
            border = if (isSelected) 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else 
                BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection checkbox
                AnimatedVisibility(
                    visible = isSelectionMode,
                    enter = scaleIn() + fadeIn(),
                    exit = scaleOut() + fadeOut()
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { onClick() },
                        modifier = Modifier.padding(end = 12.dp)
                    )
                }
                
                // Note content
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = note.title.ifEmpty { "Untitled" },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        
                        // Status indicators
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (note.isPinned) {
                                Icon(
                                    Icons.Default.Star,
                                    contentDescription = "Pinned",
                                    tint = Color(0xFFFFD700),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (note.isFavorite) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "Favorite",
                                    tint = Color(0xFFFC5C7D),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            if (note.isArchived) {
                                Icon(
                                    Icons.Default.Archive,
                                    contentDescription = "Archived",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                    
                    if (note.content.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = note.content,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatRelativeTime(note.updatedAt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (note.tags.isNotEmpty()) {
                            Surface(
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = note.tags.split(",").first().trim(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyNotesState(
    filterType: NoteFilterType,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            when (filterType) {
                NoteFilterType.PINNED -> Icons.Default.Star
                NoteFilterType.FAVORITES -> Icons.Default.Favorite
                NoteFilterType.VAULT -> Icons.Default.Archive
                NoteFilterType.RECENT -> Icons.Default.Schedule
                else -> Icons.Default.Note
            },
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = when (filterType) {
                NoteFilterType.PINNED -> "No pinned notes"
                NoteFilterType.FAVORITES -> "No favorite notes"  
                NoteFilterType.VAULT -> "No archived notes"
                NoteFilterType.RECENT -> "No recent notes"
                else -> "No notes yet"
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Text(
            text = when (filterType) {
                NoteFilterType.PINNED -> "Pin important notes to see them here"
                NoteFilterType.FAVORITES -> "Mark notes as favorites to see them here"
                NoteFilterType.VAULT -> "Archived notes will appear here"
                NoteFilterType.RECENT -> "Notes from the last 24 hours will appear here"
                else -> "Create your first note to get started"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

private fun formatRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60_000 -> "Just now"
        diff < 3600_000 -> "${diff / 60_000}m ago"
        diff < 86400_000 -> "${diff / 3600_000}h ago"
        diff < 604800_000 -> "${diff / 86400_000}d ago"
        else -> {
            val sdf = java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault())
            sdf.format(java.util.Date(timestamp))
        }
    }
}