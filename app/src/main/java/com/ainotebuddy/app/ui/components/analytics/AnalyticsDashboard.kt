package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.NoteActivity
import com.ainotebuddy.app.data.model.analytics.TagUsage
import com.ainotebuddy.app.ui.theme.*
import com.ainotebuddy.app.ui.viewmodel.AnalyticsViewModel
import androidx.compose.foundation.rememberScrollState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.max

/**
 * The main analytics dashboard that displays various statistics and visualizations.
 *
 * @param viewModel The ViewModel that provides data and handles business logic
 * @param modifier Modifier to be applied to the layout
 * @param onNavigateToTagAnalytics Callback when navigating to tag analytics
 * @param onNavigateToActivityHeatmap Callback when navigating to activity heatmap
 * @param onExportReport Callback when exporting a report
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AnalyticsDashboard(
    viewModel: AnalyticsViewModel,
    modifier: Modifier = Modifier,
    onNavigateToTagAnalytics: () -> Unit = {},
    onNavigateToActivityHeatmap: () -> Unit = {},
    onExportReport: () -> Unit = {}
) {
    // Collect state from ViewModel
    val uiState by viewModel.uiState.collectAsState()
    val tagUsage by viewModel.tagUsage.collectAsState(emptyList())
    val activityHeatmap by viewModel.activityHeatmap.collectAsState(emptyMap())
    val noteStats by viewModel.noteStats.collectAsState(emptyMap())
    val selectedTimeRange by viewModel.selectedTimeRange.collectAsState()
    
    // Track scroll state for app bar elevation
    val scrollState = rememberScrollState()
    val showElevation by remember {
        derivedStateOf { scrollState.value > 0 }
    }
    
    // Data will be loaded explicitly via selector and retry; avoid duplicate triggers
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Time range selector
        TimeRangeSelector(
            selectedRange = selectedTimeRange,
            onRangeSelected = { viewModel.loadAnalyticsData(it) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        )
        
        // Main content
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            val errorMessage = uiState.error
            // Show loading indicator when loading
            if (uiState.isLoading) {
                com.ainotebuddy.app.ui.components.LoadingIndicator()
            } else if (errorMessage != null) {
                // Show error message
                ErrorMessage(
                    message = errorMessage,
                    onRetry = { viewModel.loadAnalyticsData(selectedTimeRange) }
                )
            } else {
                // Main content
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Stats Overview
                    item {
                        StatsOverview(
                            noteStats = noteStats,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Activity Chart
                    item {
                        ActivityChart(
                            activityData = noteStats.entries
                                .sortedBy { it.key.ordinal }
                                .map { it.key.displayName to it.value },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(250.dp)
                        )
                    }
                    
                    // Tag Usage
                    item {
                        TagUsageSection(
                            tags = tagUsage,
                            onViewAllTags = onNavigateToTagAnalytics,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Activity Heatmap
                    item {
                        ActivityHeatmapSection(
                            heatmapData = activityHeatmap,
                            onViewHeatmap = onNavigateToActivityHeatmap,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    // Add some bottom padding
                    item {
                        Spacer(modifier = Modifier.height(80.dp)) // For FAB
                    }
                }
            }
            
            // Floating action button for exporting reports
            if (!uiState.isLoading && uiState.error == null) {
                FloatingActionButton(
                    onClick = onExportReport,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Export Report"
                    )
                }
            }
        }
    }
}



@Composable
private fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp)
            .wrapContentSize(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Error loading analytics",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Button(onClick = onRetry) {
            Text("Retry")
        }
    }
}

/*
@Composable
private fun AnalyticsContent(
    tagUsage: List<TagUsage>,
    activityHeatmap: List<ActivityHeatmap>,
    modifier: Modifier = Modifier
) {
    val totalNotes = tagUsage.sumOf { it.count }
    val mostUsedTag = tagUsage.maxByOrNull { it.count }?.tag ?: "None"
    val activityByHour = activityHeatmap.groupBy { it.hour }
        .mapValues { (_, activities) -> activities.sumOf { it.activityCount } }

    // Calculate max activity for scaling
    val maxActivity = (activityByHour.values.maxOrNull() ?: 0).takeIf { it > 0 } ?: 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Analytics",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Time range selector
                    TimeRangeSelector(
                        selectedRange = selectedTimeRange,
                        onRangeSelected = { selectedTimeRange = it },
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            // Quick Stats Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Notes Created Card
                    StatCard(
                        title = "Total Notes",
                        value = totalNotes.toString(),
                        icon = Icons.Default.Note,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Purple500, Indigo500)
                    )

                    // Tags Used Card
                    StatCard(
                        title = "Tags Used",
                        value = tagUsage.size.toString(),
                        icon = Icons.Default.Label,
                        modifier = Modifier.weight(1f),
                        gradientColors = listOf(Teal500, Green500)
                    )
                }
            }

            // Activity Overview Card
            item {
                AnalyticsCard(
                    title = "Activity Overview",
                    icon = Icons.Default.Timeline,
                    onActionClick = { showExportDialog = true }
                ) {
                    // Activity chart
                    Text(
                        "Hourly Activity",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Bar chart for hourly activity
                    ActivityBarChart(
                        data = activityByHour,
                        maxValue = maxActivity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .padding(vertical = 8.dp)
                    )

                    // Time range selector
                    TimeRangeSelector(
                        selectedRange = selectedTimeRange,
                        onRangeSelected = { selectedTimeRange = it },
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            // Tag Usage Card
            item {
                AnalyticsCard(
                    title = "Tag Usage",
                    icon = Icons.Default.Label,
                    onActionClick = onNavigateToTagAnalytics
                ) {
                    if (tagUsage.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.LabelOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "No tags used yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    } else {
                        // Tag cloud or list view
                        val maxTagCount = tagUsage.maxOfOrNull { it.count }?.toFloat() ?: 1f

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            tagUsage.forEach { tag ->
                                val scale = 0.8f + (tag.count.toFloat() / maxTagCount) * 0.8f
                                val animatedScale by animateFloatAsState(
                                    targetValue = scale,
                                    label = "tagScale"
                                )

                                Surface(
                                    shape = MaterialTheme.shapes.medium,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier
                                        .clip(MaterialTheme.shapes.medium)
                                        .clickable { onNavigateToTagAnalytics() }
                                        .graphicsLayer {
                                            scaleX = animatedScale
                                            scaleY = animatedScale
                                        }
                                        .padding(4.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "#${tag.tag}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            tag.count.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier
                                                .background(
                                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    shape = CircleShape
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Show view all button if there are many tags
                        if (tagUsage.size > 5) {
                            TextButton(
                                onClick = onNavigateToTagAnalytics,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text("View All Tags (${tagUsage.size})")
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            item {
                AnalyticsCard(
                    title = "Activity Heatmap",
                    icon = Icons.Default.TrendingUp,
                    onActionClick = onNavigateToActivityHeatmap
                ) {
                    // Simple heatmap preview
                    // Full heatmap would be in a dedicated screen
                    Text("Activity over time")
                    // Add a simple bar chart preview
                }
            }

            // Add more analytics cards as needed

            item {
                Spacer(modifier = Modifier.height(80.dp)) // Add bottom padding for FAB
            }
        }

        // Floating action button for exporting
        FloatingActionButton(
            onClick = { showExportDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(
                imageVector = Icons.Default.FileDownload,
                contentDescription = "Export Report"
            )
        }
    }
}

*/

/*
@Composable
private fun AnalyticsCard(
    title: String,
    icon: ImageVector,
    onActionClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(
                    onClick = onActionClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun TagUsageItem(tag: TagUsage) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable { /* Handle tag click */ }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "#${tag.tag}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "${tag.count} notes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ExportReportDialog(
    selectedFormat: ExportFormat,
    onFormatSelected: (ExportFormat) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    dateRange: Pair<Int, Int>,
    onDateRangeSelected: (Pair<Int, Int>) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Report") },
        text = {
            Column {
                Text("Select export format:")
                ExportFormat.values().forEach { format ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = format == selectedFormat,
                            onClick = { onFormatSelected(format) }
                        )
                        Text(format.name, modifier = Modifier.padding(start = 8.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Date range:")
                // Add date range selector
                // This is a simplified version - in a real app you'd use a date picker
                val ranges = listOf(
                    "Last 7 days" to (7 to 7),
                    "Last 30 days" to (30 to 30),
                    "Last 90 days" to (90 to 90),
                    "Custom" to (0 to 0)
                )
                
                ranges.forEach { (label, range) ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        RadioButton(
                            selected = dateRange == range,
                            onClick = { onDateRangeSelected(range) }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Export")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ActivityBarChart(
    data: Map<Int, Int>,
    maxValue: Int,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    barWidth: Dp = 12.dp,
    labelTextStyle: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.labelSmall
) {
    val hours = (0..23).toList()
    val chartData = hours.map { hour -> data[hour] ?: 0 }
    
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
    ) {
        val chartHeight = size.height - 20.dp.toPx() // Leave space for labels
        val barSpacing = (size.width - (barWidth.toPx() * hours.size)) / (hours.size - 1)
        
        // Draw grid lines
        
        // Draw horizontal grid lines
        for (i in 0..4) {
            val y = size.height - 20.dp.toPx() - (i * (chartHeight / 4))
            drawLine(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)
            )
        }
        
        // Draw bars
        hours.forEachIndexed { index, hour ->
            val value = chartData[index]
            val barHeight = if (maxValue > 0) {
                (value.toFloat() / maxValue) * chartHeight
            } else {
                0f
            }
            
            val left = index * (barWidth.toPx() + barSpacing)
            val top = size.height - 20.dp.toPx() - barHeight
            val right = left + barWidth.toPx()
            val bottom = size.height - 20.dp.toPx()
            
            // Draw bar
            drawRoundRect(
                color = barColor,
                topLeft = Offset(left, top),
                size = androidx.compose.ui.geometry.Size(barWidth.toPx(), barHeight),
                cornerRadius = CornerRadius(4f, 4f)
            )
            
            // Draw hour label
            if (hour % 3 == 0) { // Only show every 3 hours to avoid clutter
                val hourText = if (hour == 0) "12AM" 
                    else if (hour < 12) "$hour" 
                    else if (hour == 12) "12PM" 
                    else "${hour - 12}"
                
                drawContext.canvas.nativeCanvas.drawText(
                    hourText,
                    left + (barWidth.toPx() / 2) - 10,
                    size.height - 5.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 10f
                        color = MaterialTheme.colorScheme.onSurfaceVariant.toArgb()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    gradientColors: List<Color>
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        shadowElevation = 2.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = gradientColors,
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    ),
                    alpha = 0.1f
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = gradientColors.first(),
                    modifier = Modifier.size(20.dp)
                )
                
                Column {
                    Text(
                        text = value,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
*/
@Composable
private fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val timeRanges = remember { TimeRange.values() }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Row(
            modifier = Modifier
                .height(36.dp)
                .padding(2.dp)
        ) {
            timeRanges.forEach { range ->
                val isSelected = range == selectedRange
                val backgroundColor = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    Color.Transparent
                }
                
                val textColor = if (isSelected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(backgroundColor)
                        .clickable { onRangeSelected(range) }
                        .padding(horizontal = 12.dp)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = range.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = textColor,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

// NOTE: Central TimeRange lives in TimeRange.kt. Removed duplicate to avoid conflicts.

// Extension property to get display name for ActivityType
private val ActivityType.displayName: String
    get() = when (this) {
        ActivityType.CREATED -> "Created"
        ActivityType.UPDATED -> "Updated"
        ActivityType.VIEWED -> "Viewed"
        ActivityType.DELETED -> "Deleted"
        ActivityType.EDITED -> "Edited"
    }
private val Green500 = Color(0xFF4CAF50)
