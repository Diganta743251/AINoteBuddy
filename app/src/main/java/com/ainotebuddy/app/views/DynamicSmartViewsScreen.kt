package com.ainotebuddy.app.views

import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ainotebuddy.app.data.NoteEntity
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import kotlinx.coroutines.launch

/**
 * Dynamic Smart Views Screen - Main interface for intelligent note organization
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicSmartViewsScreen(
    onBack: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    viewModel: DynamicViewsViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    // State
    val smartViews by viewModel.smartViews.collectAsState()
    val viewSuggestions by viewModel.viewSuggestions.collectAsState()
    val currentParadigm by viewModel.currentParadigm.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val userPreferences by viewModel.userPreferences.collectAsState()
    
    // UI State
    var showSuggestions by remember { mutableStateOf(false) }
    var showParadigmSelector by remember { mutableStateOf(false) }
    var selectedView by remember { mutableStateOf<SmartView?>(null) }
    
    // Load views on first composition
    LaunchedEffect(Unit) {
        viewModel.refreshViews()
    }
    
    AINoteBuddyTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { 
                        Column {
                            Text(
                                text = "Smart Views",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Organized by ${currentParadigm.displayName}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // Paradigm selector
                        IconButton(onClick = { showParadigmSelector = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Change organization")
                        }
                        
                        // Suggestions indicator
                        if (viewSuggestions.isNotEmpty()) {
                            BadgedBox(
                                badge = {
                                    Badge {
                                        Text(viewSuggestions.size.toString())
                                    }
                                }
                            ) {
                                IconButton(onClick = { showSuggestions = true }) {
                                    Icon(Icons.Default.Lightbulb, contentDescription = "AI Suggestions")
                                }
                            }
                        }
                        
                        // Refresh
                        IconButton(
                            onClick = { 
                                scope.launch { viewModel.refreshViews() }
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    isLoading -> {
                        LoadingIndicator()
                    }
                    smartViews.isEmpty() -> {
                        EmptyViewsState(
                            onCreateView = { viewModel.createCustomView() }
                        )
                    }
                    else -> {
                        SmartViewsList(
                            views = smartViews,
                            onViewClick = { view ->
                                selectedView = view
                            },
                            onNoteClick = onNoteClick,
                            onCustomizeView = { view ->
                                viewModel.showCustomizationDialog(view)
                            }
                        )
                    }
                }
                
                // Suggestions Bottom Sheet
                if (showSuggestions) {
                    SuggestionsBottomSheet(
                        suggestions = viewSuggestions,
                        onDismiss = { showSuggestions = false },
                        onApplySuggestion = { suggestion ->
                            scope.launch {
                                viewModel.applySuggestion(suggestion)
                                showSuggestions = false
                            }
                        }
                    )
                }
                
                // Paradigm Selector Dialog
                if (showParadigmSelector) {
                    ParadigmSelectorDialog(
                        currentParadigm = currentParadigm,
                        onDismiss = { showParadigmSelector = false },
                        onParadigmSelected = { paradigm ->
                            scope.launch {
                                viewModel.changeParadigm(paradigm)
                                showParadigmSelector = false
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartViewsList(
    views: List<SmartView>,
    onViewClick: (SmartView) -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onCustomizeView: (SmartView) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(views, key = { it.id }) { view ->
            SmartViewCard(
                view = view,
                onClick = { onViewClick(view) },
                onNoteClick = onNoteClick,
                onCustomize = { onCustomizeView(view) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmartViewCard(
    view: SmartView,
    onClick: () -> Unit,
    onNoteClick: (NoteEntity) -> Unit,
    onCustomize: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = when (view.priority) {
                ViewPriority.CRITICAL -> MaterialTheme.colorScheme.errorContainer
                ViewPriority.HIGH -> MaterialTheme.colorScheme.primaryContainer
                ViewPriority.MEDIUM -> MaterialTheme.colorScheme.secondaryContainer
                ViewPriority.LOW -> MaterialTheme.colorScheme.surfaceVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = getIconForView(view),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = view.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = view.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                Row {
                    // Priority indicator
                    if (view.priority == ViewPriority.CRITICAL) {
                        Icon(
                            Icons.Default.PriorityHigh,
                            contentDescription = "High Priority",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Customize button
                    IconButton(
                        onClick = onCustomize,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = "Customize",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    
                    // Expand/collapse button
                    IconButton(
                        onClick = { expanded = !expanded },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
            
            // Expandable content
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp)
                ) {
                    // View metadata
                    if (view.metadata.isNotEmpty()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            view.metadata.forEach { (key, value) ->
                                AssistChip(
                                    onClick = { },
                                    label = { Text("$key: $value", fontSize = 10.sp) },
                                    modifier = Modifier.height(24.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    
                    // Notes preview
                    Text(
                        text = "Recent Notes",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    view.notes.take(3).forEach { note ->
                        NotePreviewItem(
                            note = note,
                            onClick = { onNoteClick(note) }
                        )
                    }
                    
                    if (view.notes.size > 3) {
                        TextButton(
                            onClick = onClick,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("View all ${view.notes.size} notes")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotePreviewItem(
    note: NoteEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = note.title.ifEmpty { note.content.take(50) + "..." },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = formatTimeAgo(note.createdAt),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Organizing your notes...",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EmptyViewsState(
    onCreateView: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.ViewModule,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "No Smart Views Yet",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = "Create some notes first, and AI will automatically organize them into smart views",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = onCreateView
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Custom View")
            }
        }
    }
}

// Helper functions
private fun getIconForView(view: SmartView): ImageVector {
    return when (view.icon) {
        "today" -> Icons.Default.Today
        "date_range" -> Icons.Default.DateRange
        "schedule" -> Icons.Default.Schedule
        "topic" -> Icons.Default.Topic
        "priority_high" -> Icons.Default.PriorityHigh
        "event" -> Icons.Default.Event
        "flash_on" -> Icons.Default.FlashOn
        "work" -> Icons.Default.Work
        "sentiment_satisfied" -> Icons.Default.SentimentSatisfied
        "sentiment_neutral" -> Icons.Default.SentimentNeutral
        "sentiment_dissatisfied" -> Icons.Default.SentimentDissatisfied
        else -> Icons.Default.ViewModule
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60 * 1000 -> "Just now"
        diff < 60 * 60 * 1000 -> "${diff / (60 * 1000)}m ago"
        diff < 24 * 60 * 60 * 1000 -> "${diff / (60 * 60 * 1000)}h ago"
        diff < 7 * 24 * 60 * 60 * 1000 -> "${diff / (24 * 60 * 60 * 1000)}d ago"
        else -> "${diff / (7 * 24 * 60 * 60 * 1000)}w ago"
    }
}

// Extension property for paradigm display names
private val OrganizationParadigm.displayName: String
    get() = when (this) {
        OrganizationParadigm.TIME_BASED -> "Time"
        OrganizationParadigm.TOPIC_BASED -> "Topics"
        OrganizationParadigm.PRIORITY_BASED -> "Priority"
        OrganizationParadigm.PROJECT_BASED -> "Projects"
        OrganizationParadigm.SENTIMENT_BASED -> "Sentiment"
        OrganizationParadigm.INTELLIGENT_AUTO -> "AI Auto"
    }
