package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A selector for choosing a time range for analytics data.
 *
 * @param selectedRange The currently selected time range
 * @param onRangeSelected Callback when a time range is selected
 * @param modifier Modifier to be applied to the layout
 * @param timeRanges List of available time ranges to display (defaults to common ranges)
 * @param selectedItemColor Background color of the selected item
 * @param unselectedItemColor Background color of unselected items
 * @param selectedTextColor Text color of the selected item
 * @param unselectedTextColor Text color of unselected items
 */
@Composable
fun TimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier,
    timeRanges: List<TimeRange> = TimeRange.values().toList(),
    selectedItemColor: Color = MaterialTheme.colorScheme.primary,
    unselectedItemColor: Color = MaterialTheme.colorScheme.surfaceVariant,
    selectedTextColor: Color = MaterialTheme.colorScheme.onPrimary,
    unselectedTextColor: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        timeRanges.forEach { range ->
            TimeRangeItem(
                range = range,
                isSelected = range == selectedRange,
                onSelected = { onRangeSelected(range) },
                selectedBackgroundColor = selectedItemColor,
                unselectedBackgroundColor = unselectedItemColor,
                selectedTextColor = selectedTextColor,
                unselectedTextColor = unselectedTextColor,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun TimeRangeItem(
    range: TimeRange,
    isSelected: Boolean,
    onSelected: () -> Unit,
    selectedBackgroundColor: Color,
    unselectedBackgroundColor: Color,
    selectedTextColor: Color,
    unselectedTextColor: Color,
    modifier: Modifier = Modifier
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) selectedBackgroundColor else Color.Transparent,
        label = "timeRangeBg_${range.name}"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) selectedTextColor else unselectedTextColor,
        label = "timeRangeText_${range.name}"
    )
    
    Surface(
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onSelected),
        color = backgroundColor,
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = range.displayName,
            color = textColor,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .wrapContentWidth()
        )
    }
}

/**
 * Represents a time range for filtering analytics data.
 */
// NOTE: TimeRange is defined in TimeRange.kt. This duplicate enum was removed to avoid conflicts.
