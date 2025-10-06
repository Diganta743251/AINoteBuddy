package com.ainotebuddy.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.ui.viewmodels.EnhancedNotesViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PinnedNotesScreen(
    onNoteClick: (NoteEntity) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EnhancedNotesViewModel = hiltViewModel()
) {
    FilteredNotesScreenContent(
        title = "Pinned Notes",
        icon = Icons.Default.Star,
        emptyMessage = "No pinned notes",
        emptyDescription = "Pin important notes to access them quickly",
        filterType = NoteFilterType.PINNED,
        onNoteClick = onNoteClick,
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoriteNotesScreen(
    onNoteClick: (NoteEntity) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EnhancedNotesViewModel = hiltViewModel()
) {
    FilteredNotesScreenContent(
        title = "Favorite Notes",
        icon = Icons.Default.Favorite,
        emptyMessage = "No favorite notes",
        emptyDescription = "Mark notes as favorites to see them here",
        filterType = NoteFilterType.FAVORITES,
        onNoteClick = onNoteClick,
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultNotesScreen(
    onNoteClick: (NoteEntity) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EnhancedNotesViewModel = hiltViewModel()
) {
    FilteredNotesScreenContent(
        title = "Vault",
        icon = Icons.Default.Archive,
        emptyMessage = "No archived notes",
        emptyDescription = "Archived notes will appear here for safekeeping",
        filterType = NoteFilterType.VAULT,
        onNoteClick = onNoteClick,
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecentNotesScreen(
    onNoteClick: (NoteEntity) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EnhancedNotesViewModel = hiltViewModel()
) {
    FilteredNotesScreenContent(
        title = "Recent Notes",
        icon = Icons.Default.Schedule,
        emptyMessage = "No recent notes",
        emptyDescription = "Notes from the last 24 hours will appear here",
        filterType = NoteFilterType.RECENT,
        onNoteClick = onNoteClick,
        onNavigateBack = onNavigateBack,
        viewModel = viewModel
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilteredNotesScreenContent(
    title: String,
    icon: ImageVector,
    emptyMessage: String,
    emptyDescription: String,
    filterType: NoteFilterType,
    onNoteClick: (NoteEntity) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: EnhancedNotesViewModel
) {
    // Apply filter when filterType changes
    LaunchedEffect(filterType) {
        viewModel.onFilterChange(filterType)
    }
    
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val filteredNotes = remember(notes, filterType) {
        when (filterType) {
            NoteFilterType.PINNED -> notes.filter { it.isPinned }
            NoteFilterType.FAVORITES -> notes.filter { it.isFavorite }
            NoteFilterType.VAULT -> notes.filter { it.isArchived }
            NoteFilterType.RECENT -> {
                val dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000)
                notes.filter { it.updatedAt > dayAgo }
            }
            else -> notes
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = title,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Text(
                        text = "${filteredNotes.size} ${if (filteredNotes.size == 1) "note" else "notes"}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (filteredNotes.isEmpty()) {
                EmptyFilteredNotesState(
                    icon = icon,
                    message = emptyMessage,
                    description = emptyDescription,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredNotes, key = { it.id }) { note ->
                        CompactNoteCard(
                            note = note,
                            onClick = { onNoteClick(note) },
                            onPinClick = { viewModel.onPinClick(note) },
                            onFavoriteClick = { viewModel.onFavoriteClick(note) },
                            onArchiveClick = { viewModel.onArchiveClick(note) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyFilteredNotesState(
    icon: ImageVector,
    message: String,
    description: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactNoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onPinClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onArchiveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
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
                    modifier = Modifier.weight(1f)
                )
                
                // Quick actions
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onPinClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = if (note.isPinned) "Unpin" else "Pin",
                            tint = if (note.isPinned) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onFavoriteClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (note.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (note.isFavorite) "Unfavorite" else "Favorite",
                            tint = if (note.isFavorite) 
                                MaterialTheme.colorScheme.error 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    IconButton(
                        onClick = onArchiveClick,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (note.isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            contentDescription = if (note.isArchived) "Restore" else "Archive",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            if (note.content.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = note.content,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
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
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)
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