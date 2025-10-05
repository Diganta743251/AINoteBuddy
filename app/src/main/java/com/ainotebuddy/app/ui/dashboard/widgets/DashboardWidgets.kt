package com.ainotebuddy.app.ui.dashboard.widgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.ui.components.GlassCard
import com.ainotebuddy.app.ui.dashboard.DashboardWidgetConfig
import com.ainotebuddy.app.ui.dashboard.DashboardWidgetType
import com.ainotebuddy.app.viewmodel.NoteViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun DashboardWidget(
    config: DashboardWidgetConfig,
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit = {},
    onActionClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    when (config.type) {
        DashboardWidgetType.QUICK_STATS -> QuickStatsWidget(
            viewModel = viewModel,
            modifier = modifier
        )
        DashboardWidgetType.RECENT_NOTES -> RecentNotesWidget(
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            modifier = modifier
        )
        DashboardWidgetType.PINNED_NOTES -> PinnedNotesWidget(
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            modifier = modifier
        )
        DashboardWidgetType.FAVORITE_NOTES -> FavoriteNotesWidget(
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            modifier = modifier
        )
        DashboardWidgetType.QUICK_ACTIONS -> QuickActionsWidget(
            onActionClick = onActionClick,
            modifier = modifier
        )
        DashboardWidgetType.AI_SUGGESTIONS -> EnhancedAISuggestionsWidget(
            onSuggestionClick = { suggestion ->
                // Handle AI suggestion click
                onActionClick()
            },
            onViewAllClick = {
                // Navigate to full AI insights screen
                onActionClick()
            },
            modifier = modifier
        )
        DashboardWidgetType.UPCOMING_REMINDERS -> UpcomingRemindersWidget(
            viewModel = viewModel,
            onNoteClick = onNoteClick,
            modifier = modifier
        )
        DashboardWidgetType.CATEGORIES_OVERVIEW -> CategoriesOverviewWidget(
            viewModel = viewModel,
            modifier = modifier
        )
        DashboardWidgetType.SEARCH_SHORTCUTS -> SearchShortcutsWidgetContainer(
            onActionClick = onActionClick,
            modifier = modifier
        )
        DashboardWidgetType.PRODUCTIVITY_STATS -> ProductivityStatsWidget(
            viewModel = viewModel,
            modifier = modifier
        )
    }
}

@Composable
fun QuickStatsWidget(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val favoriteNotes = remember(notes) { notes.filter { it.isFavorite } }
    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }
    val vaultNotes = remember(notes) { notes.filter { it.isInVault } }

    WidgetContainer(
        title = "Quick Stats",
        icon = Icons.Filled.Analytics,
        color = Color(0xFF6A82FB),
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                StatCard(
                    title = "Total",
                    value = notes.size.toString(),
                    icon = Icons.Filled.Note,
                    color = Color(0xFF6A82FB)
                )
            }
            item {
                StatCard(
                    title = "Favorites",
                    value = favoriteNotes.size.toString(),
                    icon = Icons.Filled.Favorite,
                    color = Color(0xFFFC5C7D)
                )
            }
            item {
                StatCard(
                    title = "Pinned",
                    value = pinnedNotes.size.toString(),
                    icon = Icons.Filled.Star,
                    color = Color(0xFFFFD700)
                )
            }
            item {
                StatCard(
                    title = "Vault",
                    value = vaultNotes.size.toString(),
                    icon = Icons.Filled.Lock,
                    color = Color(0xFF00FFC6)
                )
            }
        }
    }
}

@Composable
fun RecentNotesWidget(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()

    WidgetContainer(
        title = "Recent Notes",
        icon = Icons.Filled.Schedule,
        color = Color(0xFF667eea),
        modifier = modifier
    ) {
        if (notes.isEmpty()) {
            EmptyStateMessage("No notes yet")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notes.take(3).forEach { note ->
                    CompactNoteItem(
                        note = note,
                        onClick = { onNoteClick(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun PinnedNotesWidget(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val pinnedNotes = remember(notes) { notes.filter { it.isPinned } }

    WidgetContainer(
        title = "Pinned Notes",
        icon = Icons.Filled.Star,
        color = Color(0xFFFFD700),
        modifier = modifier
    ) {
        if (pinnedNotes.isEmpty()) {
            EmptyStateMessage("No pinned notes")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                pinnedNotes.take(3).forEach { note ->
                    CompactNoteItem(
                        note = note,
                        onClick = { onNoteClick(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteNotesWidget(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val favoriteNotes = remember(notes) { notes.filter { it.isFavorite } }

    WidgetContainer(
        title = "Favorite Notes",
        icon = Icons.Filled.Favorite,
        color = Color(0xFFFC5C7D),
        modifier = modifier
    ) {
        if (favoriteNotes.isEmpty()) {
            EmptyStateMessage("No favorite notes")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                favoriteNotes.take(3).forEach { note ->
                    CompactNoteItem(
                        note = note,
                        onClick = { onNoteClick(note) }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionsWidget(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WidgetContainer(
        title = "Quick Actions",
        icon = Icons.Filled.Speed,
        color = Color(0xFF00FFC6),
        modifier = modifier
    ) {
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            item {
                QuickActionButton(
                    title = "Templates",
                    icon = Icons.Filled.Description,
                    color = Color(0xFF9C27B0),
                    onClick = onActionClick
                )
            }
            item {
                QuickActionButton(
                    title = "Categories",
                    icon = Icons.Filled.Folder,
                    color = Color(0xFF4CAF50),
                    onClick = onActionClick
                )
            }
            item {
                QuickActionButton(
                    title = "Voice Note",
                    icon = Icons.Filled.Mic,
                    color = Color(0xFFFF5722),
                    onClick = onActionClick
                )
            }
        }
    }
}

@Composable
fun AISuggestionsWidget(
    modifier: Modifier = Modifier
) {
    WidgetContainer(
        title = "AI Suggestions",
        icon = Icons.Filled.Psychology,
        color = Color(0xFF9C27B0),
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SuggestionItem(
                text = "Review notes from last week",
                icon = Icons.Filled.Schedule
            )
            SuggestionItem(
                text = "Organize untagged notes",
                icon = Icons.Filled.Label
            )
            SuggestionItem(
                text = "Create meeting template",
                icon = Icons.Filled.BusinessCenter
            )
        }
    }
}

@Composable
fun UpcomingRemindersWidget(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val notesWithReminders = notes.filter { it.reminderTime != null && it.reminderTime!! > System.currentTimeMillis() }
    
    WidgetContainer(
        title = "Upcoming Reminders",
        icon = Icons.Filled.Notifications,
        color = Color(0xFFFF9800),
        modifier = modifier
    ) {
        if (notesWithReminders.isEmpty()) {
            EmptyStateMessage("No upcoming reminders")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                notesWithReminders.take(3).forEach { note ->
                    CompactNoteItem(
                        note = note,
                        onClick = { onNoteClick(note) },
                        showReminder = true
                    )
                }
            }
        }
    }
}

@Composable
fun CategoriesOverviewWidget(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val categoryCounts = notes.groupBy { it.category }.mapValues { it.value.size }
    
    WidgetContainer(
        title = "Categories",
        icon = Icons.Filled.Category,
        color = Color(0xFF4CAF50),
        modifier = modifier
    ) {
        if (categoryCounts.isEmpty()) {
            EmptyStateMessage("No categories")
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                categoryCounts.entries.take(4).forEach { (category, count) ->
                    CategoryItem(
                        name = category,
                        count = count
                    )
                }
            }
        }
    }
}

@Composable
fun SearchShortcutsWidgetContainer(
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Delegate to the package-level SearchShortcutsWidget implementation
    SearchShortcutsWidget(
        onSearchClick = { query ->
            // Navigate to search with the query
            onActionClick()
        },
        onViewAllClick = {
            // Navigate to full search screen
            onActionClick()
        },
        modifier = modifier
    )
}

@Composable
fun SearchShortcutsWidgetSimple(
    onSearchClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WidgetContainer(
        title = "Search Shortcuts",
        icon = Icons.Filled.Search,
        color = Color(0xFF2196F3),
        modifier = modifier
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SearchShortcutItem(
                text = "Untagged Notes",
                icon = Icons.Filled.LabelOff,
                onClick = { onSearchClick("tag:none") }
            )
            SearchShortcutItem(
                text = "Favorites",
                icon = Icons.Filled.Favorite,
                onClick = { onSearchClick("is:fav") }
            )
            SearchShortcutItem(
                text = "Last 7 Days",
                icon = Icons.Filled.Schedule,
                onClick = { onSearchClick("updated:7d") }
            )
            TextButton(onClick = onViewAllClick) {
                Text("View all searches")
            }
        }
    }
}

@Composable
fun ProductivityStatsWidget(
    viewModel: NoteViewModel,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState()
    val todayNotes = notes.filter { 
        val today = System.currentTimeMillis()
        val noteDate = it.createdAt
        (today - noteDate) < 24 * 60 * 60 * 1000 // Last 24 hours
    }
    
    WidgetContainer(
        title = "Today's Activity",
        icon = Icons.Filled.TrendingUp,
        color = Color(0xFFE91E63),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ProductivityStat(
                label = "Notes",
                value = todayNotes.size.toString(),
                icon = Icons.Filled.Note
            )
            ProductivityStat(
                label = "Words",
                value = todayNotes.sumOf { it.wordCount }.toString(),
                icon = Icons.Filled.TextFields
            )
        }
    }
}

// Helper Composables

@Composable
fun WidgetContainer(
    title: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    GlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun CompactNoteItem(
    note: NoteEntity,
    onClick: () -> Unit,
    showReminder: Boolean = false
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = note.title.ifBlank { "Untitled" },
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (showReminder && note.reminderTime != null) {
                    Text(
                        text = formatReminderTime(note.reminderTime!!),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFF9800)
                    )
                } else {
                    Text(
                        text = formatDate(note.updatedAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            Row {
                if (note.isFavorite) {
                    Icon(
                        Icons.Filled.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFFC5C7D),
                        modifier = Modifier.size(12.dp)
                    )
                }
                if (note.isPinned) {
                    Icon(
                        Icons.Filled.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun QuickActionButton(
    title: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun SuggestionItem(
    text: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { }
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF9C27B0),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun CategoryItem(
    name: String,
    count: Int
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f),
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF4CAF50)
        )
    }
}

@Composable
fun SearchShortcutItem(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF2196F3),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun ProductivityStat(
    label: String,
    value: String,
    icon: ImageVector
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFFE91E63),
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun EmptyStateMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodySmall,
        color = Color.White.copy(alpha = 0.6f),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

// Helper functions
private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun formatReminderTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}