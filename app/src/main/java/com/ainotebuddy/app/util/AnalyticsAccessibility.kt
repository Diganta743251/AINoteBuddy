package com.ainotebuddy.app.util

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.*
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.data.model.analytics.ActivityType
import com.ainotebuddy.app.data.model.analytics.TagUsage
import com.ainotebuddy.app.theme.AnalyticsTheme
import java.text.NumberFormat

/**
 * Accessibility utility functions for the analytics feature
 */
object AnalyticsAccessibility {
    
    // Content descriptions for screen readers
    const val ANALYTICS_SCREEN = "Analytics Screen"
    const val TIME_RANGE_SELECTOR = "Time Range Selector"
    const val REFRESH_BUTTON = "Refresh Data"
    const val EXPORT_BUTTON = "Export Report"
    const val BACK_BUTTON = "Navigate Up"
    
    // State descriptions
    const val LOADING_STATE = "Loading analytics data"
    const val ERROR_STATE = "Error loading analytics"
    const val EMPTY_STATE = "No data available"
    
    // Chart accessibility
    const val CHART_CONTAINER = "Chart showing data"
    const val CHART_DATA_POINT = "Data point"
    
    /**
     * Get content description for a tag chip
     */
    fun getTagContentDescription(tag: TagUsage): String {
        return "Tag ${tag.tag}, used ${tag.count} times"
    }
    
    /**
     * Get content description for an activity type
     */
    fun getActivityTypeDescription(activityType: ActivityType): String {
        return when (activityType) {
            ActivityType.CREATED -> "Note Created"
            ActivityType.UPDATED -> "Note Updated"
            ActivityType.VIEWED -> "Note Viewed"
            ActivityType.DELETED -> "Note Deleted"
            else -> activityType.name.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }
        }
    }
    
    /**
     * Get content description for a time range
     */
    fun getTimeRangeDescription(timeRange: String): String {
        return "Time range: $timeRange"
    }
    
    /**
     * Get content description for a data point in a chart
     */
    fun getDataPointDescription(label: String, value: Number, total: Number? = null): String {
        val formattedValue = NumberFormat.getNumberInstance().format(value)
        return if (total != null) {
            val percentage = (value.toDouble() / total.toDouble() * 100).toInt()
            "$label: $formattedValue, $percentage% of total"
        } else {
            "$label: $formattedValue"
        }
    }
}

/**
 * Composable that adds accessibility support to analytics charts
 */
@Composable
fun AccessibleChartContainer(
    chartName: String,
    chartDescription: String = "",
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    Column(
        modifier = modifier
            .semantics(mergeDescendants = true) {
                // Set the chart as a group for accessibility
                this.role = Role.Tab
                this.contentDescription = "$chartName chart. $chartDescription"
                this.isTraversalGroup = true
            }
    ) {
        // Add a hidden heading for screen readers
        Text(
            text = "$chartName Chart",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .semantics { 
                    invisibleToUser()
                    heading()
                }
        )
        
        // The actual chart content
        content()
    }
}

/**
 * Accessible stat card for displaying analytics metrics
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccessibleStatCard(
    title: String,
    value: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    description: String = ""
) {
    val context = LocalContext.current
    
    Card(
        onClick = { /* Handle click for accessibility */ },
        modifier = modifier
            .semantics(mergeDescendants = true) {
                this.role = Role.Button
                this.contentDescription = "$title: $value. $description"
                this.isTraversalGroup = true
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.medium
                    )
                    .padding(8.dp)
            ) {
                icon()
            }
        }
    }
}

/**
 * Accessible time range selector
 */
@Composable
fun AccessibleTimeRangeSelector(
    selectedRange: String,
    onRangeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val ranges = listOf("Week", "Month", "Quarter", "Year", "All Time")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .semantics(mergeDescendants = true) {
                this.role = Role.Tab
                this.contentDescription = "Select time range"
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ranges.forEach { range ->
            val isSelected = range == selectedRange
            
            FilterChip(
                selected = isSelected,
                onClick = { onRangeSelected(range) },
                label = { Text(range) },
                modifier = Modifier
                    .semantics {
                        selected = isSelected
                        this.contentDescription = "$range time range" + if (isSelected) ", selected" else ""
                    },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

/**
 * Accessible loading indicator
 */
@Composable
fun AccessibleLoadingIndicator(
    loadingText: String = "Loading...",
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                this.contentDescription = loadingText
                this.liveRegion = LiveRegionMode.Polite
            }
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
    }
}

/**
 * Accessible error message
 */
@Composable
fun AccessibleErrorMessage(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = "Error: $message"
                this.liveRegion = LiveRegionMode.Assertive
            }
    ) {
        Icon(
            imageVector = Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(48.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onRetry,
                modifier = Modifier.semantics {
                    this.contentDescription = "Retry loading data"
                }
            ) {
                Text("Retry")
            }
        }
    }
}

/**
 * Accessible empty state
 */
@Composable
fun AccessibleEmptyState(
    message: String,
    modifier: Modifier = Modifier,
    action: @Composable (() -> Unit)? = null
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp)
            .semantics(mergeDescendants = true) {
                this.contentDescription = message
            }
    ) {
        Icon(
            imageVector = Icons.Default.Analytics,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(64.dp)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = message,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        if (action != null) {
            Spacer(modifier = Modifier.height(16.dp))
            action()
        }
    }
}
