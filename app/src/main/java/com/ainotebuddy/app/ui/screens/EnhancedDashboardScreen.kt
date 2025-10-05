package com.ainotebuddy.app.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.viewmodel.NoteViewModel
import com.ainotebuddy.app.ui.components.*
import com.ainotebuddy.app.ui.theme.*
 
import java.text.SimpleDateFormat
import java.util.*

data class DashboardWidget(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val onClick: () -> Unit
)

data class QuickStat(
    val label: String,
    val value: String,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedDashboardScreen(
    viewModel: NoteViewModel,
    onNoteClick: (NoteEntity) -> Unit,
    onNewNote: () -> Unit,
    onSearch: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onTagClick: (String) -> Unit,
    onTemplates: () -> Unit,
    onCategories: () -> Unit,
    onShowAllNotes: () -> Unit,
    onShowFavorites: () -> Unit,
    onShowPinned: () -> Unit,
    onShowVault: () -> Unit,
    onCustomizeDashboard: () -> Unit,
    onCustomizeFAB: () -> Unit,
    onShowPresets: () -> Unit,
    modifier: Modifier = Modifier
) {
    val notes by viewModel.notes.collectAsState(initial = emptyList())
    val windowSizeClass = rememberWindowSizeClass()
    var selectedTimeFilter by remember { mutableStateOf(TimeFilter.ALL) }
    var showQuickActions by remember { mutableStateOf(false) }
    
    // Calculate statistics
    val stats = remember(notes) {
        calculateNoteStats(notes)
    }
    
    // Recent notes (last 7 days)
    val recentNotes = remember(notes, selectedTimeFilter) {
        filterNotesByTime(notes, selectedTimeFilter).take(6)
    }
    
    // Quick action widgets
    val quickActions = remember {
        listOf(
            DashboardWidget(
                id = "new_note",
                title = "New Note",
                icon = Icons.Default.Add,
                color = ModernColors.AIPrimary,
                onClick = onNewNote
            ),
            DashboardWidget(
                id = "search",
                title = "Search",
                icon = Icons.Default.Search,
                color = ModernColors.AISecondary,
                onClick = onSearch
            ),
            DashboardWidget(
                id = "voice_note",
                title = "Voice Note",
                icon = Icons.Default.Mic,
                color = ModernColors.Success,
                onClick = { /* Handle voice note */ }
            ),
            DashboardWidget(
                id = "templates",
                title = "Templates",
                icon = Icons.Default.Description,
                color = ModernColors.Warning,
                onClick = onTemplates
            ),
            DashboardWidget(
                id = "ai_assist",
                title = "AI Assist",
                icon = Icons.Default.Psychology,
                color = ModernColors.Info,
                onClick = { /* Handle AI assist */ }
            ),
            DashboardWidget(
                id = "collaboration",
                title = "Collaborate",
                icon = Icons.Default.Group,
                color = ModernColors.NoteWork,
                onClick = { /* Handle collaboration */ }
            )
        )
    }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = ResponsivePadding(windowSizeClass),
        verticalArrangement = Arrangement.spacedBy(ResponsiveSpacing(windowSizeClass))
    ) {
        // Welcome header
        item {
            WelcomeHeader(
                userName = "User", // Get from user preferences
                totalNotes = notes.size,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Quick stats
        item {
            QuickStatsSection(
                stats = stats,
                windowSizeClass = windowSizeClass,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Quick actions
        item {
            QuickActionsSection(
                actions = quickActions,
                windowSizeClass = windowSizeClass,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // AI suggestions
        item {
            AISuggestionsSection(
                onSuggestionClick = { suggestion ->
                    // Handle AI suggestion
                },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Recent notes section
        item {
            RecentNotesSection(
                notes = recentNotes,
                onNoteClick = onNoteClick,
                onSeeAll = onShowAllNotes,
                timeFilter = selectedTimeFilter,
                onTimeFilterChange = { selectedTimeFilter = it },
                windowSizeClass = windowSizeClass,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Categories overview
        item {
            CategoriesOverviewSection(
                notes = notes,
                onCategoryClick = onCategoryClick,
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        // Productivity insights
        item {
            ProductivityInsightsSection(
                notes = notes,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun WelcomeHeader(
    userName: String,
    totalNotes: Int,
    modifier: Modifier = Modifier
) {
    val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    val greeting = when (currentHour) {
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                        )
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text(
                    text = "$greeting, $userName!",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "You have $totalNotes notes in your collection",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault()).format(Date()),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun QuickStatsSection(
    stats: List<QuickStat>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    val columns = when (windowSizeClass.widthSizeClass) {
        ScreenSize.COMPACT -> 2
        ScreenSize.MEDIUM -> 3
        ScreenSize.EXPANDED -> 4
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            val rows = (stats.size + columns - 1) / columns
            LazyVerticalGrid(
                columns = GridCells.Fixed(columns),
                modifier = Modifier.height((rows * 80).dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(stats) { stat ->
                    QuickStatCard(stat = stat)
                }
            }
        }
    }
}

@Composable
private fun QuickStatCard(
    stat: QuickStat,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = stat.color.copy(alpha = 0.1f)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = stat.icon,
                contentDescription = null,
                tint = stat.color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = stat.value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = stat.label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    actions: List<DashboardWidget>,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Actions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            when (windowSizeClass.widthSizeClass) {
                ScreenSize.COMPACT -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(200.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(actions.take(4)) { action ->
                            QuickActionItemCard(action = action)
                        }
                    }
                }
                else -> {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(actions) { action ->
                            QuickActionItemCard(action = action)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickActionItemCard(
    action: DashboardWidget,
    modifier: Modifier = Modifier
) {
    QuickActionCard(
        title = action.title,
        description = "",
        icon = action.icon,
        onClick = action.onClick,
        modifier = modifier.width(120.dp),
        accentColor = action.color
    )
}

@Composable
private fun AISuggestionsSection(
    onSuggestionClick: (AISuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    val suggestions = remember {
        listOf(
                AISuggestion(
                    id = "organize",
                    title = "Organize Notes",
                    description = "Group similar notes together",
                    icon = Icons.Default.Group,
                    confidence = 0.9f,
                    actionType = AIActionType.ORGANIZE
                ),
            AISuggestion(
                id = "summary",
                title = "Weekly Summary",
                description = "Create a summary of this week's notes",
                icon = Icons.Default.Summarize,
                confidence = 0.8f,
                actionType = AIActionType.SUMMARIZE
            ),
            AISuggestion(
                id = "brainstorm",
                title = "Brainstorm Ideas",
                description = "Generate new ideas based on your notes",
                icon = Icons.Default.Lightbulb,
                confidence = 0.7f,
                actionType = AIActionType.BRAINSTORM
            )
        )
    }
    
    AnimatedVisibility(
        visible = suggestions.isNotEmpty(),
        enter = slideInVertically() + fadeIn(),
        exit = slideOutVertically() + fadeOut()
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "AI Suggestions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.semanticColors().aiPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                AISuggestionChips(
                    suggestions = suggestions,
                    onSuggestionClick = onSuggestionClick
                )
            }
        }
    }
}

@Composable
private fun RecentNotesSection(
    notes: List<NoteEntity>,
    onNoteClick: (NoteEntity) -> Unit,
    onSeeAll: () -> Unit,
    timeFilter: TimeFilter,
    onTimeFilterChange: (TimeFilter) -> Unit,
    windowSizeClass: WindowSizeClass,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Notes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var showFilterMenu by remember { mutableStateOf(false) }
                    TextButton(onClick = { showFilterMenu = true }) {
                        Text(timeFilter.displayName)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false }
                    ) {
                        TimeFilter.values().forEach { filter ->
                            DropdownMenuItem(
                                text = { Text(filter.displayName) },
                                onClick = {
                                    onTimeFilterChange(filter)
                                    showFilterMenu = false
                                }
                            )
                        }
                    }
                    TextButton(onClick = onSeeAll) { Text("See All") }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (notes.isEmpty()) {
                EmptyStateMessage(message = "No recent notes found", icon = Icons.Default.Note)
            } else {
                when (windowSizeClass.widthSizeClass) {
                    ScreenSize.COMPACT -> {
                        LazyColumn(
                            modifier = Modifier.height(300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notes) { note ->
                                CompactNoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note) }
                                )
                            }
                        }
                    }
                    else -> {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier.height(400.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(notes) { note ->
                                CompactNoteCard(
                                    note = note,
                                    onClick = { onNoteClick(note) },
                                    onFavorite = {},
                                    onPin = {}
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
private fun CategoriesOverviewSection(
    notes: List<NoteEntity>,
    onCategoryClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryCounts = remember(notes) {
        notes.groupBy { it.category }.mapValues { it.value.size }
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (categoryCounts.isEmpty()) {
                EmptyStateMessage(message = "No categories yet", icon = Icons.Default.Category)
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(categoryCounts.entries.toList()) { (category, count) ->
                        CategoryCard(
                            category = category,
                            count = count,
                            onClick = { onCategoryClick(category) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductivityInsightsSection(
    notes: List<NoteEntity>,
    modifier: Modifier = Modifier
) {
    val insights = remember(notes) {
        generateProductivityInsights(notes)
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Productivity Insights",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            insights.forEach { insight ->
                InsightCard(
                    insight = insight,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}

// Local shims for missing components used by this screen
@Composable
private fun EmptyStateMessage(message: String, icon: ImageVector? = null) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun CompactNoteCard(
    note: NoteEntity,
    onClick: () -> Unit,
    onFavorite: (() -> Unit)? = null,
    onPin: (() -> Unit)? = null
) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(note.title.ifBlank { "Untitled" }, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(note.content.take(120) + if (note.content.length > 120) "..." else "", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CategoryCard(
    category: String,
    count: Int,
    onClick: () -> Unit
) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Row(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(category, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text("$count notes", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

enum class TimeFilter(val displayName: String) {
    ALL("All Time"),
    TODAY("Today"),
    WEEK("This Week"),
    MONTH("This Month")
}

data class ProductivityInsight(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

@Composable
private fun InsightCard(
    insight: ProductivityInsight,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = insight.icon,
                contentDescription = null,
                tint = insight.color,
                modifier = Modifier.size(24.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = insight.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (insight.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = insight.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun calculateNoteStats(notes: List<NoteEntity>): List<QuickStat> {
    val today = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
    
    val todayNotes = notes.count { it.createdAt >= today }
    val favoriteNotes = notes.count { it.isFavorite }
    val pinnedNotes = notes.count { it.isPinned }
    val totalWords = notes.sumOf { it.content.split("\\s+".toRegex()).size }
    
    return listOf(
        QuickStat("Total Notes", notes.size.toString(), Icons.Default.Note, ModernColors.AIPrimary),
        QuickStat("Today", todayNotes.toString(), Icons.Default.Today, ModernColors.Success),
        QuickStat("Favorites", favoriteNotes.toString(), Icons.Default.Favorite, ModernColors.Error),
        QuickStat("Words", totalWords.toString(), Icons.Default.Article, ModernColors.Warning)
    )
}

private fun filterNotesByTime(notes: List<NoteEntity>, filter: TimeFilter): List<NoteEntity> {
    val calendar = Calendar.getInstance()
    val cutoffTime = when (filter) {
        TimeFilter.ALL -> return notes
        TimeFilter.TODAY -> {
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis
        }
        TimeFilter.WEEK -> {
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.timeInMillis
        }
        TimeFilter.MONTH -> {
            calendar.add(Calendar.MONTH, -1)
            calendar.timeInMillis
        }
    }
    
    return notes.filter { it.updatedAt >= cutoffTime }
        .sortedByDescending { it.updatedAt }
}

private fun generateProductivityInsights(notes: List<NoteEntity>): List<ProductivityInsight> {
    return listOf(
        ProductivityInsight(
            title = "Great Progress!",
            description = "You've created ${notes.size} notes this month",
            icon = Icons.Default.TrendingUp,
            color = ModernColors.Success
        ),
        ProductivityInsight(
            title = "Stay Organized",
            description = "Consider organizing your notes with tags",
            icon = Icons.Default.Group,
            color = ModernColors.Info
        )
    )
}

// Additional helper composables would go here...
