package com.ainotebuddy.app.util

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * Animation specifications for the analytics feature
 */
object AnalyticsAnimations {
    // Standard animation durations
    const val SHORT_ANIM_DURATION = 200
    const val MEDIUM_ANIM_DURATION = 300
    const val LONG_ANIM_DURATION = 500
    
    // Standard animation specs
    val standardEasing = FastOutSlowInEasing
    val standardTween = tween<Float>(
        durationMillis = MEDIUM_ANIM_DURATION,
        easing = standardEasing
    )
    
    // Fade in animation
    val fadeIn = fadeIn(animationSpec = standardTween)
    
    // Fade out animation
    val fadeOut = fadeOut(animationSpec = standardTween)
    
    // Slide in from bottom animation
    val slideInFromBottom = slideInVertically(
        animationSpec = tween(durationMillis = MEDIUM_ANIM_DURATION, easing = standardEasing),
        initialOffsetY = { fullHeight: Int -> fullHeight / 2 }
    )
    
    // Slide out to bottom animation
    val slideOutToBottom = slideOutVertically(
        animationSpec = tween(durationMillis = MEDIUM_ANIM_DURATION, easing = standardEasing),
        targetOffsetY = { fullHeight: Int -> fullHeight / 2 }
    )
    
    // Scale in animation
    val scaleIn = scaleIn(
        animationSpec = standardTween,
        initialScale = 0.9f
    )
    
    // Scale out animation
    val scaleOut = scaleOut(
        animationSpec = standardTween,
        targetScale = 0.9f
    )
    
    // Combined fade and slide up animation
    val fadeInSlideUp = fadeIn(animationSpec = tween(durationMillis = MEDIUM_ANIM_DURATION, easing = standardEasing)) + slideInVertically(
        animationSpec = tween(durationMillis = MEDIUM_ANIM_DURATION, easing = standardEasing),
        initialOffsetY = { fullHeight: Int -> fullHeight / 4 }
    )
    
    // Combined fade and slide down animation
    val fadeOutSlideDown = fadeOut(animationSpec = tween(durationMillis = MEDIUM_ANIM_DURATION, easing = standardEasing)) + slideOutVertically(
        animationSpec = tween(durationMillis = MEDIUM_ANIM_DURATION, easing = standardEasing),
        targetOffsetY = { fullHeight: Int -> fullHeight / 4 }
    )
}

/**
 * Animated visibility with standard analytics animations
 */
@Composable
fun AnalyticsAnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = AnalyticsAnimations.fadeInSlideUp,
    exit: ExitTransition = AnalyticsAnimations.fadeOutSlideDown,
    content: @Composable AnimatedVisibilityScope.() -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = enter,
        exit = exit,
        modifier = modifier,
        content = content
    )
}

/**
 * Animate content when data is loaded
 */
@Composable
fun <T> AnimateOnDataLoad(
    data: T?,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = { AnalyticsLoadingPlaceholder() },
    content: @Composable (T) -> Unit
) {
    AnimatedContent(
        targetState = data,
        transitionSpec = {
            if (targetState != null && initialState == null) {
                // Fade in when data loads
                fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 100))
            } else if (targetState == null && initialState != null) {
                // Fade out when data is cleared
                fadeIn(animationSpec = tween(durationMillis = 100)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 300))
            } else {
                // Crossfade when data changes
                fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 300))
            }.using(SizeTransform(clip = false))
        },
        modifier = modifier
    ) { targetData ->
        if (targetData != null) {
            content(targetData)
        } else {
            placeholder()
        }
    }
}

/**
 * Loading placeholder with shimmer effect
 */
@Composable
fun AnalyticsLoadingPlaceholder(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
) {
    var alpha by remember { mutableStateOf(0.2f) }
    LaunchedEffect(Unit) {
        while (true) {
            alpha = 0.6f
            kotlinx.coroutines.delay(500)
            alpha = 0.2f
            kotlinx.coroutines.delay(500)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(color.copy(alpha = alpha))
    )
}

/**
 * Animated transition for time range selector
 */
@Composable
fun AnimatedTimeRangeSelector(
    selectedRange: TimeRange,
    onRangeSelected: (TimeRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(selectedRange, label = "timeRangeTransition")
    
    val backgroundColor by transition.animateColor(
        transitionSpec = { tween(durationMillis = 300) },
        label = "backgroundColor"
    ) { range ->
        when (range) {
            TimeRange.DAY -> MaterialTheme.colorScheme.secondaryContainer
            TimeRange.WEEK -> MaterialTheme.colorScheme.primaryContainer
            TimeRange.MONTH -> MaterialTheme.colorScheme.tertiaryContainer
            TimeRange.YEAR -> MaterialTheme.colorScheme.primaryContainer
            TimeRange.ALL_TIME -> MaterialTheme.colorScheme.tertiaryContainer
        }
    }
    
    // Inline simple selector to avoid unresolved reference
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TimeRange.values().forEach { rangeOption ->
            val isSelected = rangeOption == selectedRange
            androidx.compose.material3.AssistChip(
                onClick = { onRangeSelected(rangeOption) },
                label = { Text(rangeOption.name.replace("_", " ")) },
                colors = androidx.compose.material3.AssistChipDefaults.assistChipColors(
                    containerColor = if (isSelected) backgroundColor else MaterialTheme.colorScheme.surfaceVariant
                )
            )
        }
    }
}

/**
 * Animated error message
 */
@Composable
fun AnimatedErrorText(
    message: String,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = modifier.padding(8.dp)
        )
    }
}

/**
 * Animated refresh indicator
 */
@Composable
fun AnimatedRefreshIndicator(
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    // Use a simple infinite transition to avoid misusing Animatable.animateTo with infiniteRepeatable
    val infiniteTransition = rememberInfiniteTransition(label = "refreshRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isRefreshing) 0.6f else 1f,
        animationSpec = tween(durationMillis = 200),
        label = "alphaAnim"
    )

    androidx.compose.material3.Icon(
        imageVector = Icons.Filled.Refresh,
        contentDescription = if (isRefreshing) "Refreshing..." else "Refresh",
        modifier = modifier
            .rotate(if (isRefreshing) rotation else 0f)
            .alpha(alpha)
    )
}

/**
 * Animated chart data point
 */
@Composable
fun AnimatedChartPoint(
    value: Float,
    maxValue: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedHeight by animateFloatAsState(
        targetValue = value / maxValue,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "chartPointAnimation"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(animatedHeight)
            .background(
                color = color,
                shape = MaterialTheme.shapes.small
            )
    )
}

/**
 * Animated counter for statistics
 */
@Composable
fun AnimatedCounter(
    count: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium
) {
    var oldCount by remember { mutableStateOf(count) }
    val animatedCount = animateIntAsState(
        targetValue = count,
        animationSpec = tween(
            durationMillis = 1000,
            easing = FastOutSlowInEasing
        ),
        label = "counterAnimation"
    )
    
    LaunchedEffect(count) {
        oldCount = count
    }
    
    Text(
        text = animatedCount.value.toString(),
        style = style,
        modifier = modifier
    )
}
