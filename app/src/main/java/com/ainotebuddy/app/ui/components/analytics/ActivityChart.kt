package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalDensity
import kotlin.math.max

/**
 * A bar chart component for displaying activity data.
 *
 * @param activityData List of pairs where first is the activity name and second is the count
 * @param modifier Modifier to be applied to the layout
 * @param barColor Color of the bars
 * @param labelColor Color of the labels
 * @param maxBars Maximum number of bars to show (default 7)
 * @param barSpacing Spacing between bars
 * @param showLabels Whether to show labels under each bar
 */
@Composable
fun ActivityChart(
    activityData: List<Pair<String, Int>>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    maxBars: Int = 7,
    barSpacing: Dp = 8.dp,
    showLabels: Boolean = true
) {
    if (activityData.isEmpty()) {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No activity data available",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    val maxValue = (activityData.maxOfOrNull { it.second } ?: 0).takeIf { it > 0 } ?: 1
    val barData = activityData.take(maxBars)
    val barSpacingPx = with(LocalDensity.current) { barSpacing.toPx() }
    
    // Animation for the bars
    val animationProgress by animateFloatAsState(
        targetValue = 1f,
        label = "chartAnimation"
    )

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chart area
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val chartHeightPx = with(LocalDensity.current) { maxHeight.toPx() }
            val chartWidth = maxWidth
            val barWidth = (chartWidth - (barSpacing * (barData.size - 1))) / barData.size
            
            // Draw bars
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = if (showLabels) 24.dp else 0.dp)
            ) {
                barData.forEachIndexed { index, (_, value) ->
                    val barHeight = (value.toFloat() / maxValue) * chartHeightPx * animationProgress
                    val startX = index * (barWidth + barSpacing).toPx()
                    
                    drawRect(
                        color = barColor,
                        topLeft = Offset(
                            x = startX,
                            y = size.height - barHeight
                        ),
                        size = Size(
                            width = barWidth.toPx(),
                            height = barHeight
                        ),
                        style = Stroke(width = 2.dp.toPx())
                    )
                    
                    drawRect(
                        color = barColor.copy(alpha = 0.3f),
                        topLeft = Offset(
                            x = startX,
                            y = size.height - barHeight
                        ),
                        size = Size(
                            width = barWidth.toPx(),
                            height = barHeight
                        )
                    )
                }
            }
            
            // Draw value labels
            if (showLabels) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    barData.forEach { (label, _) ->
                        Text(
                            text = label,
                            color = labelColor,
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .width(barWidth)
                                .padding(horizontal = 2.dp)
                        )
                    }
                }
            }
        }
        
        // X-axis labels
        if (showLabels) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "0",
                    color = labelColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
                
                Text(
                    text = maxValue.toString(),
                    color = labelColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontSize = 10.sp
                )
            }
        }
    }
}
