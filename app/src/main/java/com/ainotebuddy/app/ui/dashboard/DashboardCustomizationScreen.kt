package com.ainotebuddy.app.ui.dashboard

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
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
fun DashboardCustomizationScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val widgetManager = remember { DashboardWidgetManager(context) }
    val scope = rememberCoroutineScope()
    
    var widgets by remember { mutableStateOf(widgetManager.getAllWidgets()) }
    var showResetDialog by remember { mutableStateOf(false) }
    
    // Update widgets when configuration changes
    LaunchedEffect(widgetManager.currentConfig.value) {
        widgets = widgetManager.getAllWidgets()
    }
    
    val reorderableState = rememberReorderableLazyListState(
        onMove = { from, to ->
            widgets = widgets.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
        },
        onDragEnd = { fromIndex, toIndex ->
            scope.launch {
                widgetManager.reorderWidgets(fromIndex, toIndex)
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
                    text = "Customize Dashboard",
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
        
        // Instructions
        GlassCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        text = "Customize Your Dashboard",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Toggle widgets on/off using the switches\n• Drag and drop to reorder widgets\n• Changes are saved automatically",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.8f),
                    lineHeight = 18.sp
                )
            }
        }
        
        // Widget List
        LazyColumn(
            state = reorderableState.listState,
            modifier = Modifier
                .fillMaxSize()
                .reorderable(reorderableState)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            itemsIndexed(widgets, key = { _, widget -> widget.type.id }) { index, widget ->
                ReorderableItem(
                    reorderableState = reorderableState,
                    key = widget.type.id
                ) { isDragging ->
                    val elevation by animateDpAsState(
                        targetValue = if (isDragging) 8.dp else 0.dp,
                        label = "elevation"
                    )
                    
                    WidgetConfigurationItem(
                        widget = widget,
                        isDragging = isDragging,
                        elevation = elevation,
                        onToggle = { enabled ->
                            scope.launch {
                                widgetManager.toggleWidget(widget.type, enabled)
                            }
                        },
                        modifier = Modifier.detectReorderAfterLongPress(reorderableState)
                    )
                }
            }
        }
    }
    
    // Reset Dialog
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = {
                Text(
                    text = "Reset Dashboard",
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "This will reset your dashboard to the default configuration. Are you sure?",
                    color = Color.White.copy(alpha = 0.8f)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            widgetManager.resetToDefault()
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
fun WidgetConfigurationItem(
    widget: DashboardWidgetConfig,
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
            
            // Widget Icon
            Icon(
                widget.type.icon,
                contentDescription = null,
                tint = widget.type.color,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Widget Info
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = widget.type.displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White
                )
                Text(
                    text = widget.type.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            
            // Toggle Switch
            Switch(
                checked = widget.isEnabled,
                onCheckedChange = onToggle,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = widget.type.color,
                    uncheckedThumbColor = Color.White.copy(alpha = 0.6f),
                    uncheckedTrackColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}

@Composable
fun WidgetPreview(
    widgetType: DashboardWidgetType,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.size(80.dp),
        colors = CardDefaults.cardColors(
            containerColor = widgetType.color.copy(alpha = 0.2f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                widgetType.icon,
                contentDescription = null,
                tint = widgetType.color,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}