package com.ainotebuddy.app.ui.dashboard

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.ui.components.AIActionType
import com.ainotebuddy.app.viewmodel.NoteViewModel

/**
 * AI-Integrated Dashboard that showcases all AI features in a unified interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AIIntegratedDashboard(
    viewModel: NoteViewModel = hiltViewModel(),
    onNavigateToNoteEditor: () -> Unit = {},
    onNavigateToSearch: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {}
) {
    var selectedTab by remember { mutableStateOf(DashboardTab.OVERVIEW) }
    var showAIInsights by remember { mutableStateOf(false) }

    // Collect data from ViewModel
    val notes by viewModel.notes.collectAsState()
    val isLoading = false

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        // Top App Bar with AI branding
        AIIntegratedTopBar(
            onSearchClick = onNavigateToSearch,
            onSettingsClick = onNavigateToSettings
        )

        // Tab Navigation
        AITabNavigation(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )

        // Main Content based on selected tab
        when (selectedTab) {
            DashboardTab.OVERVIEW -> {
                AIOverviewContent(
                    notes = notes,
                                        onCreateNote = onNavigateToNoteEditor
                )
            }
            DashboardTab.INSIGHTS -> {
                AIInsightsContent(
                    notes = notes
                )
            }
            DashboardTab.SEARCH -> {
                AISearchContent(
                    onNavigateToFullSearch = onNavigateToSearch
                )
            }
            DashboardTab.PRODUCTIVITY -> {
                AIProductivityContent(
                    notes = notes
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AIIntegratedTopBar(
    onSearchClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "AI NotesBuddy",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Search"
                )
            }
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Settings"
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    )
}

@Composable
private fun AITabNavigation(
    selectedTab: DashboardTab,
    onTabSelected: (DashboardTab) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(DashboardTab.values()) { tab ->
            AITabChip(
                tab = tab,
                isSelected = selectedTab == tab,
                onClick = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun AITabChip(
    tab: DashboardTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.title,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = tab.title,
                style = MaterialTheme.typography.labelMedium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AIOverviewContent(
    notes: List<NoteEntity>,
    onCreateNote: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Quick Stats Card
        item {
            AIQuickStatsCard(notes = notes)
        }

        // AI Suggestions Card
        item {
            AISuggestionsCard(notes = notes)
        }

        // Recent Notes with AI Insights
        item {
            AIRecentNotesCard(
                notes = notes.take(5),
                onCreateNote = onCreateNote
            )
        }

        // Productivity Insights
        item {
            AIProductivityCard(notes = notes)
        }
    }
}

@Composable
private fun AIInsightsContent(
    notes: List<NoteEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "AI Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Sentiment Analysis Overview
        item {
            AISentimentOverviewCard(notes = notes)
        }

        // Content Analysis
        item {
            AIContentAnalysisCard(notes = notes)
        }

        // Recommendations
        item {
            AIRecommendationsCard(notes = notes)
        }
    }
}

@Composable
private fun AISearchContent(
    onNavigateToFullSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.ManageSearch,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "AI-Powered Search",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Search your notes with natural language and get intelligent suggestions",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onNavigateToFullSearch,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Open AI Search")
        }
    }
}

@Composable
private fun AIProductivityContent(
    notes: List<NoteEntity>
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Productivity Insights",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        // Writing Habits
        item {
            AIWritingHabitsCard(notes = notes)
        }

        // Goal Tracking
        item {
            AIGoalTrackingCard(notes = notes)
        }

        // Time Management
        item {
            AITimeManagementCard(notes = notes)
        }
    }
}

@Composable
private fun AIQuickStatsCard(notes: List<NoteEntity>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Quick Stats",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem(
                    icon = Icons.Default.Note,
                    label = "Total Notes",
                    value = notes.size.toString()
                )

                StatItem(
                    icon = Icons.Default.Star,
                    label = "Starred",
                    value = notes.count { it.isFavorite }.toString()
                )

                StatItem(
                    icon = Icons.Default.Category,
                    label = "Categories",
                    value = notes.mapNotNull { it.category }.distinct().size.toString()
                )
            }
        }
    }
}

@Composable
private fun StatItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

// ... Additional composable functions for other cards would go here

enum class DashboardTab(
    val title: String,
    val icon: ImageVector
) {
    OVERVIEW("Overview", Icons.Default.Dashboard),
    INSIGHTS("Insights", Icons.Default.Psychology),
    SEARCH("Search", Icons.Default.Search),
    PRODUCTIVITY("Productivity", Icons.Default.TrendingUp)
}

// Placeholder implementations for cards that would contain actual AI logic
@Composable
private fun AISuggestionsCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Suggestions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Smart suggestions based on your notes will appear here")
        }
    }
}

@Composable
private fun AIRecentNotesCard(notes: List<NoteEntity>, onCreateNote: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Recent Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Your recent notes with AI insights")
        }
    }
}

@Composable
private fun AIProductivityCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Productivity Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("AI-calculated productivity insights")
        }
    }
}

@Composable
private fun AISentimentOverviewCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Sentiment Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Overall sentiment analysis of your notes")
        }
    }
}

@Composable
private fun AIContentAnalysisCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Content Analysis",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("AI-powered content insights and themes")
        }
    }
}

@Composable
private fun AIRecommendationsCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "AI Recommendations",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Personalized recommendations for better note-taking")
        }
    }
}

@Composable
private fun AIWritingHabitsCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Writing Habits",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Analysis of your writing patterns and habits")
        }
    }
}

@Composable
private fun AIGoalTrackingCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Goal Tracking",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("Track your progress towards goals mentioned in notes")
        }
    }
}

@Composable
private fun AITimeManagementCard(notes: List<NoteEntity>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Time Management",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text("AI insights on your time management and productivity")
        }
    }
}




