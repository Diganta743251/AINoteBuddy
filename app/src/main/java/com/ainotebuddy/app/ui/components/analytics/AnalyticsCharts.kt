package com.ainotebuddy.app.ui.components.analytics

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.theme.AnalyticsTheme
import com.ainotebuddy.app.util.AnalyticsAccessibility
import com.ainotebuddy.app.util.AnalyticsAnimations
import com.ainotebuddy.app.theme.getChartColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import kotlinx.coroutines.launch
import kotlin.math.*
 

/**
 * Data class representing a data point in a chart
 */
data class ChartDataPoint(
    val label: String,
    val value: Float,
    val color: Color = getChartColor(label.hashCode())
)

/**
 * Data class representing a segment in a pie/donut chart
 */
data class ChartSegment(
    val label: String,
    val value: Float,
    val color: Color,
    val startAngle: Float = 0f,
    val sweepAngle: Float = 0f
)

/**
 * Composable for a bar chart
 */
@Composable
fun BarChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    maxBars: Int = 7,
    showValues: Boolean = true,
    showGrid: Boolean = true,
    animationDuration: Int = 1000,
    barCornerRadius: Dp = 4.dp,
    barSpacing: Dp = 8.dp,
    barColor: Color = MaterialTheme.colorScheme.primary,
    gridColor: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
    labelTextStyle: TextStyle = MaterialTheme.typography.bodySmall,
    valueTextStyle: TextStyle = MaterialTheme.typography.bodySmall
) {
    val visibleData = if (data.size > maxBars) data.take(maxBars) else data
    val maxValue = visibleData.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1f
    val spacingPx = with(LocalDensity.current) { barSpacing.toPx() }

    // Animation state
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration, easing = LinearEasing)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Bar chart showing ${visibleData.size} data points"
            }
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
        ) {
            val density = LocalDensity.current
            val chartHeightPx = with(density) { maxHeight.toPx() - 20.dp.toPx() }
            val chartWidthPx = with(density) { maxWidth.toPx() }
            val barWidthPx = (chartWidthPx - (visibleData.size - 1) * spacingPx) / visibleData.size
            val barWidthDp = with(density) { barWidthPx.toDp() }
            val chartHeightDp = with(density) { chartHeightPx.toDp() }

            // Grid
            if (showGrid) {
                Canvas(modifier = Modifier.matchParentSize()) {
                    val gridLineCount = 5
                    repeat(gridLineCount) { i ->
                        val y = size.height * (1f - i.toFloat() / (gridLineCount - 1))
                        drawLine(
                            color = gridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                        drawContext.canvas.nativeCanvas.drawText(
                            "${(i * maxValue / (gridLineCount - 1)).toInt()}",
                            0f,
                            y + 12.dp.toPx(),
                            android.graphics.Paint().apply {
                                textSize = 10.dp.toPx()
                                color = gridColor.toArgb()
                                textAlign = android.graphics.Paint.Align.LEFT
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                visibleData.forEach { dataPoint ->
                    val animatedHeightPx = (dataPoint.value / maxValue * chartHeightPx) * animationProgress.value
                    val animatedHeightDp = with(density) { animatedHeightPx.toDp() }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .width(barWidthDp)
                            .height(chartHeightDp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(animatedHeightDp)
                                .background(
                                    color = dataPoint.color,
                                    shape = RoundedCornerShape(barCornerRadius)
                                )
                        )

                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dataPoint.label,
                            style = labelTextStyle,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        if (showValues) {
                            Text(
                                text = dataPoint.value.toInt().toString(),
                                style = valueTextStyle,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Composable for a line chart
 */
@Composable
fun LineChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillGradient: Boolean = true,
    showPoints: Boolean = true,
    showGrid: Boolean = true,
    animationDuration: Int = 1000
) {
    val maxValue = data.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1f
    val minValue = data.minOfOrNull { it.value } ?: 0f
    val valueRange = maxValue - minValue
    
    // Animation state
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            )
        )
    }
    
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = "Line chart showing ${data.size} data points"
            }
    ) {
        val chartHeight = maxHeight - 40.dp
        val chartWidth = maxWidth - 32.dp
        val pointRadius = 4.dp
        // Capture theme colors outside of draw scope
        val outlineColor = MaterialTheme.colorScheme.outline
        val surfaceColor = MaterialTheme.colorScheme.surface
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            if (data.size < 2) return@Canvas
            
            val xStep = chartWidth.toPx() / (data.size - 1)
            val yScale = chartHeight.toPx() / valueRange
            
            // Calculate points
            val points = data.mapIndexed { index, dataPoint ->
                val x = index * xStep
                val y = chartHeight.toPx() - (dataPoint.value - minValue) * yScale * animationProgress.value
                Offset(x, y.toFloat())
            }
            
            // Draw grid
            if (showGrid) {
                // Horizontal grid lines
                val gridLineCount = 5
                repeat(gridLineCount) { i ->
                    val y = chartHeight.toPx() * (1f - i.toFloat() / (gridLineCount - 1))
                    drawLine(
                        color = outlineColor.copy(alpha = 0.2f),
                        start = Offset(0f, y),
                        end = Offset(chartWidth.toPx(), y),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                
                // Vertical grid lines
                data.forEachIndexed { index, _ ->
                    val x = index * xStep
                    drawLine(
                        color = outlineColor.copy(alpha = 0.1f),
                        start = Offset(x, 0f),
                        end = Offset(x, chartHeight.toPx()),
                        strokeWidth = 1.dp.toPx()
                    )
                }
            }
            
            // Draw fill gradient
            if (fillGradient && points.isNotEmpty()) {
                val fillPath = Path().apply {
                    moveTo(0f, chartHeight.toPx())
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(points.last().x, chartHeight.toPx())
                    close()
                }
                
                drawPath(
                    path = fillPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            lineColor.copy(alpha = 0.3f),
                            lineColor.copy(alpha = 0f)
                        ),
                        startY = 0f,
                        endY = chartHeight.toPx()
                    )
                )
            }
            
            // Draw line
            val linePath = Path().apply {
                points.forEachIndexed { index, point ->
                    if (index == 0) {
                        moveTo(point.x, point.y)
                    } else {
                        val prevPoint = points[index - 1]
                        val controlPoint1 = Offset(prevPoint.x + (point.x - prevPoint.x) / 4, prevPoint.y)
                        val controlPoint2 = Offset(prevPoint.x + (point.x - prevPoint.x) * 3 / 4, point.y)
                        cubicTo(
                            controlPoint1.x, controlPoint1.y,
                            controlPoint2.x, controlPoint2.y,
                            point.x, point.y
                        )
                    }
                }
            }
            
            drawPath(
                path = linePath,
                color = lineColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            
            // Draw points
            if (showPoints) {
                points.forEach { point ->
                    drawCircle(
                        color = lineColor,
                        radius = pointRadius.toPx(),
                        center = point,
                        style = Fill
                    )
                    drawCircle(
                        color = surfaceColor,
                        radius = (pointRadius * 0.6f).toPx(),
                        center = point,
                        style = Fill
                    )
                }
            }
        }
        
        // X-axis labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 8.dp)
        ) {
            data.forEach { dataPoint ->
                Text(
                    text = dataPoint.label,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

/**
 * Composable for a pie chart
 */
@Composable
fun PieChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    showValues: Boolean = true,
    animationDuration: Int = 1000,
    holeRadiusRatio: Float = 0.6f // 0f for pie chart, >0f for donut chart
) {
    val total = data.sumOf { it.value.toDouble() }.toFloat()
    if (total <= 0) return
    
    // Calculate segments
    val segments = remember(data) {
        var startAngle = -90f // Start from top
        data.map { dataPoint ->
            val sweepAngle = (dataPoint.value / total) * 360f
            val segment = ChartSegment(
                label = dataPoint.label,
                value = dataPoint.value,
                color = dataPoint.color,
                startAngle = startAngle,
                sweepAngle = sweepAngle
            )
            startAngle += sweepAngle
            segment
        }
    }
    
    // Animation state
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            )
        )
    }
    
    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .semantics(mergeDescendants = true) {
                contentDescription = "Pie chart showing ${data.size} segments"
            },
        contentAlignment = Alignment.Center
    ) {
        val size = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val outerRadius = size / 2
        val innerRadius = outerRadius * holeRadiusRatio
        val center = Offset(size / 2, size / 2)
        // Capture theme color outside draw scope
        val surfaceColor = MaterialTheme.colorScheme.surface
        
        Canvas(modifier = Modifier.fillMaxSize()) {
            // Draw segments
            segments.forEach { segment ->
                val sweep = segment.sweepAngle * animationProgress.value
                
                // Draw segment
                drawArc(
                    color = segment.color,
                    startAngle = segment.startAngle,
                    sweepAngle = sweep,
                    useCenter = true,
                    size = Size(size, size),
                    topLeft = Offset.Zero
                )
                
                // Draw hole for donut chart
                if (holeRadiusRatio > 0) {
                    drawCircle(
                        color = surfaceColor,
                        radius = innerRadius,
                        center = center
                    )
                }
                
                // Draw segment border
                if (holeRadiusRatio > 0) {
                    drawArc(
                        color = segment.color,
                        startAngle = segment.startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        size = Size(size, size),
                        topLeft = Offset.Zero,
                        style = Stroke(width = 2.dp.toPx())
                    )
                }
            }
            
            // Draw labels
            if (showLabels) {
                segments.forEach { segment ->
                    if (segment.sweepAngle > 10) { // Only show label for large enough segments
                        val midAngle = Math.toRadians((segment.startAngle + segment.sweepAngle / 2).toDouble())
                        val labelRadius = (outerRadius + innerRadius) / 2
                        val x = center.x + (labelRadius * cos(midAngle).toFloat())
                        val y = center.y + (labelRadius * sin(midAngle).toFloat())
                        
                        drawContext.canvas.nativeCanvas.drawText(
                            segment.label,
                            x,
                            y,
                            android.graphics.Paint().apply {
                                textSize = 12.dp.toPx()
                                color = Color.White.toArgb()
                                textAlign = android.graphics.Paint.Align.CENTER
                                setShadowLayer(2f, 0f, 0f, Color.Black.toArgb())
                            }
                        )
                    }
                }
            }
        }
        
        // Center text (for donut chart)
        if (holeRadiusRatio > 0 && showValues) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "${(total * animationProgress.value).toInt()}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

/**
 * Composable for a horizontal bar chart
 */
@Composable
fun HorizontalBarChart(
    data: List<ChartDataPoint>,
    modifier: Modifier = Modifier,
    maxBars: Int = 5,
    showValues: Boolean = true,
    barHeight: Dp = 24.dp,
    barSpacing: Dp = 8.dp,
    animationDuration: Int = 1000
) {
    val visibleData = if (data.size > maxBars) data.take(maxBars) else data
    val maxValue = visibleData.maxOfOrNull { it.value }?.takeIf { it > 0 } ?: 1f
    
    // Animation state
    val animationProgress = remember { Animatable(0f) }
    
    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = animationDuration,
                easing = LinearEasing
            )
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Horizontal bar chart showing ${visibleData.size} data points"
            },
        verticalArrangement = Arrangement.spacedBy(barSpacing)
    ) {
        visibleData.forEach { dataPoint ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Label
                Text(
                    text = dataPoint.label,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                // Bar
                Box(
                    modifier = Modifier
                        .weight(2f)
                        .height(barHeight)
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.small
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth((dataPoint.value / maxValue * animationProgress.value).coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(
                                color = dataPoint.color,
                                shape = MaterialTheme.shapes.small
                            )
                    )
                    
                    // Value text
                    if (showValues) {
                        Text(
                            text = dataPoint.value.toInt().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Composable for a sparkline chart
 */
@Composable
fun SparklineChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = MaterialTheme.colorScheme.primary,
    fillGradient: Boolean = true
) {
    if (data.size < 2) return
    
    val maxValue = data.maxOrNull() ?: 1f
    val minValue = data.minOrNull() ?: 0f
    val valueRange = maxValue - minValue
    
    Canvas(modifier = modifier.height(24.dp)) {
        val width = size.width
        val height = size.height
        val xStep = width / (data.size - 1)
        
        // Calculate points
        val points = data.mapIndexed { index, value ->
            val x = index * xStep
            val y = height - ((value - minValue) / (if (valueRange > 0) valueRange else 1f)) * height
            Offset(x, y)
        }
        
        // Draw fill gradient
        if (fillGradient) {
            val fillPath = Path().apply {
                moveTo(0f, height)
                points.forEach { lineTo(it.x, it.y) }
                lineTo(width, height)
                close()
            }
            
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.3f),
                        lineColor.copy(alpha = 0f)
                    ),
                    startY = 0f,
                    endY = height
                )
            )
        }
        
        // Draw line
        val linePath = Path().apply {
            points.forEachIndexed { index, point ->
                if (index == 0) {
                    moveTo(point.x, point.y)
                } else {
                    lineTo(point.x, point.y)
                }
            }
        }
        
        drawPath(
            path = linePath,
            color = lineColor,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Draw first and last points
        if (points.isNotEmpty()) {
            // First point
            drawCircle(
                color = lineColor,
                radius = 1.5.dp.toPx(),
                center = points.first()
            )
            
            // Last point
            drawCircle(
                color = lineColor,
                radius = 2.dp.toPx(),
                center = points.last(),
                style = Stroke(width = 1.dp.toPx())
            )
        }
    }
}
