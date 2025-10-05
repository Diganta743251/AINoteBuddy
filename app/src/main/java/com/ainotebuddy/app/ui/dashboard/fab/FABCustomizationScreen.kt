package com.ainotebuddy.app.ui.dashboard.fab

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.ui.components.GlassCard
import kotlinx.coroutines.launch
import org.burnoutcrew.reorderable.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FABCustomizationScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fabManager = remember { FABConfigurationManager(context) }
    val scope = rememberCoroutineScope()
    
    var currentTab by remember { mutableStateOf(0) }
    var actions by remember { mutableStateOf(emptyList<FABActionConfig>()) }
    var menuConfig by remember { mutableStateOf(FABConfigurationManager.getDefaultFABMenuConfig()) }
    var usageStats by remember { mutableStateOf(emptyMap<String, FABActionUsage>()) }
    var suggestions by remember { mutableStateOf(emptyList<FABActionSuggestion>()) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Load configuration
    LaunchedEffect(Unit) {
        fabManager.configFlow.collect { config ->
            menuConfig = config
            actions = fabManager.getAllActions()
        }
    }
    
    LaunchedEffect(Unit) {
        fabManager.usageFlow.collect { usage ->
            usageStats = usage
            suggestions = fabManager.generateSmartSuggestions()
        }
    }
    
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            actions = actions.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { fromIndex, toIndex ->
            scope.launch {
                fabManager.reorderActions(fromIndex, toIndex)
            }
        }
    )
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1a1a2e),
                        Color(0xFF16213e),
                        Color(0xFF0f3460)
                    )
                )
            )
    ) {
        // Top App Bar
        TopAppBar(
            title = {
                Text(
                    text = "Customize Quick Actions",
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { showResetDialog = true }
                ) {
                    Icon(
                        Icons.Filled.Refresh,
                        contentDescription = "Reset to default",
                        tint = Color.White
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent
            )
        )
        
        // Tab Row
        TabRow(
            selectedTabIndex = currentTab,
            containerColor = Color.Transparent,
            contentColor = Color.White,
            indicator = { tabPositions ->
                TabRowDefaults.Indicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentTab]),
                    color = Color(0xFF6A82FB)
                )
            }
        ) {
            Tab(
                selected = currentTab == 0,
                onClick = { currentTab = 0 },
                text = { Text("Actions") }
            )
            Tab(
                selected = currentTab == 1,
                onClick = { currentTab = 1 },
                text = { Text("Layout") }
            )
            Tab(
                selected = currentTab == 2,
                onClick = { currentTab = 2 },
                text = { Text("Suggestions") }
            )
        }
        
        // Tab Content
        when (currentTab) {
            0 -> ActionsTab(
                actions = actions,
                reorderableState = reorderableState,
                onToggleAction = { action, enabled ->
                    scope.launch {
                        fabManager.toggleAction(action, enabled)
                    }
                }
            )
            1 -> LayoutTab(
                config = menuConfig,
                onStyleChange = { style ->
                    scope.launch {
                        fabManager.updateMenuStyle(style)
                    }
                },
                onMaxActionsChange = { count ->
                    scope.launch {
                        fabManager.updateMaxVisibleActions(count)
                    }
                }
            )
            2 -> SuggestionsTab(
                suggestions = suggestions,
                usageStats = usageStats,
                onApplySuggestion = { suggestion ->
                    scope.launch {
                        fabManager.toggleAction(suggestion.action, true)
                    }
                }
            )
        }
    }
    
    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset FAB Configuration",
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will reset your quick actions to the default configuration. Are you sure?",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            fabManager.resetToDefault()
                            showResetDialog = false
                        }
                    }
                ) {
                    Text("Reset", color = Color(0xFFFC5C7D))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showResetDialog = false }
                ) {
                    Text("Cancel", color = Color.White.copy(alpha = 0.7f))
                }
            },
            containerColor = Color(0xFF1a1a2e),
            tonalElevation = 8.dp
        )
    }
}

@Composable
fun ActionsTab(
    actions: List<FABActionConfig>,
    reorderableState: ReorderableLazyListState,
    onToggleAction: (FABActionType, Boolean) -> Unit
) {
    LazyColumn(
        state = reorderableState.listState,
        modifier = Modifier
            .fillMaxSize()
            .reorderable(reorderableState)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        // Instructions
        item {
            GlassCard(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = Color(0xFF6A82FB),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Customize Quick Actions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "• Toggle actions on/off using switches\n• Drag to reorder actions\n• Enabled actions appear in your FAB menu",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 18.sp
                    )
                }
            }
        }
        
        // Actions by category
        FABActionCategory.values().forEach { category ->
            val categoryActions = actions.filter { it.type.category == category }
            if (categoryActions.isNotEmpty()) {
                item {
                    CategoryHeader(category = category)
                }
                
                itemsIndexed(categoryActions, key = { _, action -> action.type.id }) { index, action ->
                    ReorderableItem(
                        reorderableState = reorderableState,
                        key = action.type.id
                    ) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "elevation"
                        )
                        
                        FABActionItem(
                            action = action,
                            isDragging = isDragging,
                            elevation = elevation,
                            onToggle = { enabled ->
                                onToggleAction(action.type, enabled)
                            },
                            modifier = Modifier.detectReorderAfterLongPress(reorderableState)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LayoutTab(
    config: FABMenuConfig,
    onStyleChange: (FABMenuStyle) -> Unit,
    onMaxActionsChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        item {
            GlassCard {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Menu Style",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    FABMenuStyle.values().forEach { style ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onStyleChange(style) }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = config.style == style,
                                onClick = { onStyleChange(style) },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF6A82FB),
                                    unselectedColor = Color.White.copy(alpha = 0.6f)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = style.name.lowercase().replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                                Text(
                                    text = getStyleDescription(style),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
        }
        
        item {
            GlassCard {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Maximum Visible Actions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Text(
                        text = "Current: ${config.maxVisibleActions}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Slider(
                        value = config.maxVisibleActions.toFloat(),
                        onValueChange = { onMaxActionsChange(it.toInt()) },
                        valueRange = 3f..8f,
                        steps = 4,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF6A82FB),
                            activeTrackColor = Color(0xFF6A82FB),
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        )
                    )
                }
            }
        }
        
        item {
            GlassCard {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Display Options",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Show Labels",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Switch(
                            checked = config.showLabels,
                            onCheckedChange = { /* Handle show labels change */ },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6A82FB),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Haptic Feedback",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White
                        )
                        Switch(
                            checked = config.hapticFeedback,
                            onCheckedChange = { /* Handle haptic feedback change */ },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF6A82FB),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SuggestionsTab(
    suggestions: List<FABActionSuggestion>,
    usageStats: Map<String, FABActionUsage>,
    onApplySuggestion: (FABActionSuggestion) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        if (suggestions.isNotEmpty()) {
            item {
                Text(
                    text = "Smart Suggestions",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            
            items(suggestions) { suggestion ->
                SuggestionCard(
                    suggestion = suggestion,
                    onApply = { onApplySuggestion(suggestion) }
                )
            }
        }
        
        if (usageStats.isNotEmpty()) {
            item {
                Text(
                    text = "Usage Statistics",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
                )
            }
            
            items(usageStats.values.sortedByDescending { it.usageCount }.take(5)) { usage ->
                UsageStatsCard(usage = usage)
            }
        }
        
        if (suggestions.isEmpty() && usageStats.isEmpty()) {
            item {
                GlassCard {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.TrendingUp,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No Data Yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Use the app more to see personalized suggestions and usage statistics",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryHeader(category: FABActionCategory) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(
            Icons.Filled.Circle,
            contentDescription = null,
            tint = category.color,
            modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = category.displayName,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun FABActionItem(
    action: FABActionConfig,
    isDragging: Boolean,
    elevation: androidx.compose.ui.unit.Dp,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) {
                Color.White.copy(alpha = 0.2f)
            } else {
                Color.White.copy(alpha = 0.1f)
            }
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isDragging) {
            androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF6A82FB))
        } else {
            androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag Handle
            Icon(
                Icons.Filled.DragHandle,
                contentDescription = "Drag to reorder",
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Action Icon
            Icon(
                action.type.icon,
                contentDescription = null,
                tint = action.type.color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Action Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = action.customLabel ?: action.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = action.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
                if (action.type.isPremium) {
                    Text(
                        text = "Premium",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            
            // Toggle Switch
            Switch(
                checked = action.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = action.type.color,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun SuggestionCard(
    suggestion: FABActionSuggestion,
    onApply: () -> Unit
) {
    GlassCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                suggestion.action.icon,
                contentDescription = null,
                tint = suggestion.action.color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = suggestion.action.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = suggestion.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(
                    containerColor = suggestion.action.color,
                    contentColor = Color.White
                ),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Add", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun UsageStatsCard(usage: FABActionUsage) {
    val actionType = FABActionType.values().find { it.id == usage.actionId }
    
    if (actionType != null) {
        GlassCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    actionType.icon,
                    contentDescription = null,
                    tint = actionType.color,
                    modifier = Modifier.size(24.dp)
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = actionType.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                    Text(
                        text = "Used ${usage.usageCount} times",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
                
                Text(
                    text = "${usage.averageUsagePerDay.toInt()}/day",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = actionType.color
                )
            }
        }
    }
}

private fun getStyleDescription(style: FABMenuStyle): String {
    return when (style) {
        FABMenuStyle.LINEAR -> "Actions in a vertical line"
        FABMenuStyle.CIRCULAR -> "Actions arranged in a circle"
        FABMenuStyle.GRID -> "Actions in a grid layout"
        FABMenuStyle.MINIMAL -> "Only show most important actions"
    }
}