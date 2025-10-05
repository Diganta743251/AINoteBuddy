package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.NoteAdd
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.data.model.analytics.ActivityType

/**
 * A composable that displays an overview of analytics statistics.
 *
 * @param noteStats Map of activity types to their counts
 * @param modifier Modifier to be applied to the layout
 * @param cardElevation Elevation of the stat cards
 * @param onStatClick Callback when a stat card is clicked
 */
@Composable
fun StatsOverview(
    noteStats: Map<ActivityType, Int>,
    modifier: Modifier = Modifier,
    cardElevation: Dp = 2.dp,
    onStatClick: (ActivityType) -> Unit = {}
) {
    val stats = listOf(
        StatItem(
            type = ActivityType.CREATED,
            count = noteStats[ActivityType.CREATED] ?: 0,
            label = "Created",
            icon = Icons.Default.NoteAdd,
            color = MaterialTheme.colorScheme.primary
        ),
        StatItem(
            type = ActivityType.UPDATED,
            count = noteStats[ActivityType.UPDATED] ?: 0,
            label = "Updated",
            icon = Icons.Default.EditNote,
            color = MaterialTheme.colorScheme.secondary
        ),
        StatItem(
            type = ActivityType.VIEWED,
            count = noteStats[ActivityType.VIEWED] ?: 0,
            label = "Viewed",
            icon = Icons.Default.Notes,
            color = MaterialTheme.colorScheme.tertiary
        ),
        StatItem(
            type = ActivityType.EDITED,
            count = noteStats[ActivityType.EDITED] ?: 0,
            label = "Edited",
            icon = Icons.Default.EditNote,
            color = MaterialTheme.colorScheme.primaryContainer
        )
    )
    
    // Calculate total activities
    val totalActivities = remember(noteStats) {
        noteStats.values.sum()
    }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Title and total
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "$totalActivities total activities",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Stats grid
        val columns = 2
        val rows = (stats.size + columns - 1) / columns
        
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(rows) { row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(columns) { col ->
                        val index = row * columns + col
                        if (index < stats.size) {
                            val stat = stats[index]
                            StatCard(
                                stat = stat,
                                elevation = cardElevation,
                                modifier = Modifier.weight(1f),
                                onClick = { onStatClick(stat.type) }
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    stat: StatItem,
    modifier: Modifier = Modifier,
    elevation: Dp = 2.dp,
    onClick: () -> Unit = {}
) {
    val animatedProgress = animateFloatAsState(
        targetValue = 1f,
        label = "stat_${stat.type}"
    )
    
    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = elevation),
        colors = CardDefaults.cardColors(
            containerColor = stat.color.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stat.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = stat.color.copy(alpha = 0.8f)
                )
                
                Icon(
                    imageVector = stat.icon,
                    contentDescription = null,
                    tint = stat.color.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }
            
            Text(
                text = stat.count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Optional: Add a small progress bar or indicator
            if (stat.total != null && stat.total > 0) {
                val progress = (stat.count.toFloat() / stat.total).coerceIn(0f, 1f)
                val animatedProgressValue by animateFloatAsState(
                    targetValue = progress * animatedProgress.value,
                    label = "progress_${stat.type}"
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(stat.color.copy(alpha = 0.2f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedProgressValue)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.dp))
                            .background(stat.color)
                    )
                }
            }
        }
    }
}

private data class StatItem(
    val type: ActivityType,
    val count: Int,
    val label: String,
    val icon: ImageVector,
    val color: Color,
    val total: Int? = null
)
