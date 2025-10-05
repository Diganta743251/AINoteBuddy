package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Top app bar for the analytics screen with back navigation, title, and refresh functionality.
 *
 * @param onBackClick Callback when the back button is clicked
 * @param isRefreshing Whether the data is currently being refreshed
 * @param onRefresh Callback when the refresh button is clicked
 * @param selectedTimeRange The currently selected time range
 * @param onTimeRangeSelected Callback when a new time range is selected
 * @param modifier Modifier to be applied to the layout
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyticsTopBar(
    onBackClick: () -> Unit,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    selectedTimeRange: TimeRange,
    onTimeRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate the elevation based on scroll state
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    
    TopAppBar(
        title = {
            Column {
                Text(
                    text = "Analytics",
                    style = MaterialTheme.typography.headlineSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Show time range selector in the app bar when scrolled
                AnimatedVisibility(visible = scrollBehavior.state.overlappedFraction > 0.5f) {
                    TimeRangeSelector(
                        selectedRange = selectedTimeRange,
                        onRangeSelected = onTimeRangeSelected,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .fillMaxWidth(0.8f),
                        selectedItemColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedItemColor = Color.Transparent,
                        selectedTextColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
        },
        actions = {
            // Refresh button
            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing
            ) {
                if (isRefreshing) {
                    // Show loading indicator when refreshing
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            }
            
            // Add some end padding
            Spacer(modifier = Modifier.width(8.dp))
        },
        scrollBehavior = scrollBehavior,
        modifier = modifier
    )
    
    // Show time range selector below app bar when not scrolled
    AnimatedVisibility(
        visible = scrollBehavior.state.overlappedFraction <= 0.5f,
        modifier = Modifier.fillMaxWidth()
    ) {
        Surface(
            tonalElevation = 2.dp,
            shadowElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            TimeRangeSelector(
                selectedRange = selectedTimeRange,
                onRangeSelected = onTimeRangeSelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                selectedItemColor = MaterialTheme.colorScheme.primary,
                unselectedItemColor = MaterialTheme.colorScheme.surfaceVariant,
                selectedTextColor = MaterialTheme.colorScheme.onPrimary,
                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
