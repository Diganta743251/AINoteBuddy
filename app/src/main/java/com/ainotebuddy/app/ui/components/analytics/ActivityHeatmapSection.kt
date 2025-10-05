package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

/**
 * A composable that displays a section for activity heatmap visualization.
 *
 * @param heatmapData Map of dates to activity counts
 * @param onViewHeatmap Callback when "View Full Heatmap" is clicked
 * @param modifier Modifier to be applied to the layout
 * @param maxWeeks Maximum number of weeks to show in the preview
 * @param onDateClick Callback when a date cell is clicked
 */
@Composable
fun ActivityHeatmapSection(
    heatmapData: Map<LocalDate, Int>,
    onViewHeatmap: () -> Unit,
    modifier: Modifier = Modifier,
    maxWeeks: Int = 4,
    onDateClick: (LocalDate) -> Unit = {}
) {
    val daysInWeek = 7
    val totalDays = maxWeeks * daysInWeek
    val today = LocalDate.now()
    val startDate = today.minusDays((totalDays - 1).toLong())
    
    // Generate all dates in the range
    val allDates = remember(heatmapData, startDate, today) {
        (0 until totalDays).map { startDate.plusDays(it.toLong()) }
    }
    
    // Calculate max activity for color scaling
    val maxActivity = remember(heatmapData) {
        (heatmapData.values.maxOrNull() ?: 0).coerceAtLeast(1)
    }
    
    // Group dates by week for layout
    val weeks = remember(allDates) {
        allDates.chunked(daysInWeek)
    }
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                Text(
                    text = "Activity Heatmap",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                TextButton(
                    onClick = onViewHeatmap,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("View Full")
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "View Full Heatmap"
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Heatmap grid
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Weekday headers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { day ->
                        Text(
                            text = day,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .weight(1f)
                                .padding(2.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Heatmap cells
                weeks.forEach { week ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        week.forEach { date ->
                            val activityCount = heatmapData[date] ?: 0
                            val isToday = date == today
                            
                            HeatmapCell(
                                activityCount = activityCount,
                                maxActivity = maxActivity,
                                isToday = isToday,
                                date = date,
                                onClick = { onDateClick(date) },
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                            )
                        }
                        
                        // Fill remaining days if week is incomplete
                        if (week.size < daysInWeek) {
                            repeat(daysInWeek - week.size) {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                )
                            }
                        }
                    }
                }
                
                // Legend
                Spacer(modifier = Modifier.height(12.dp))
                HeatmapLegend(
                    maxActivity = maxActivity,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    activityCount: Int,
    maxActivity: Int,
    isToday: Boolean,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val intensity = if (maxActivity > 0) {
        activityCount.toFloat() / maxActivity.coerceAtLeast(1)
    } else {
        0f
    }
    
    val animatedIntensity by animateFloatAsState(
        targetValue = intensity,
        label = "heatmapCell_${date}"
    )
    
    val baseColor = MaterialTheme.colorScheme.primary
    val backgroundColor = baseColor.copy(
        alpha = 0.1f + (0.7f * animatedIntensity)
    )
    
    val borderColor = if (isToday) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    
    val tooltipText = buildString {
        append(date.format(DateTimeFormatter.ofPattern("MMM d, yyyy")))
        if (activityCount > 0) {
            append("\n$activityCount ${if (activityCount == 1) "activity" else "activities"}")
        } else {
            append("\nNo activity")
        }
    }
    
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(backgroundColor)
            .border(
                width = if (isToday) 1.5.dp else 0.dp,
                color = borderColor,
                shape = RoundedCornerShape(4.dp)
            )
            .clickable(onClick = onClick)
    ) {
        // Show count for higher activity cells
        if (activityCount > 0) {
            Text(
                text = activityCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(
                    alpha = if (intensity > 0.5) 1f else 0.7f
                ),
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
private fun HeatmapLegend(
    maxActivity: Int,
    modifier: Modifier = Modifier
) {
    val baseColor = MaterialTheme.colorScheme.primary
    val steps = 5
    
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Less",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(steps) { step ->
                val intensity = step.toFloat() / (steps - 1)
                val color = baseColor.copy(alpha = 0.1f + (0.7f * intensity))
                val count = (maxActivity * intensity).toInt()
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, RoundedCornerShape(2.dp))
                    )
                    
                    if (step == steps - 1) {
                        Text(
                            text = "$count+",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (step == 0) {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
        
        Text(
            text = "More",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
