package com.ainotebuddy.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.ui.theme.ModernColors
import com.ainotebuddy.app.ui.theme.AITypography

// AI Suggestion data classes
data class AISuggestion(
    val id: String,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val confidence: Float, // 0.0 to 1.0
    val actionType: AIActionType
)

enum class AIActionType {
    SUMMARIZE, EXPAND, TRANSLATE, ORGANIZE, BRAINSTORM, CORRECT, ENHANCE
}

enum class AIThinkingState {
    IDLE, THINKING, PROCESSING, COMPLETE, ERROR
}

@Composable
fun AISuggestionChips(
    suggestions: List<AISuggestion>,
    onSuggestionClick: (AISuggestion) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(suggestions) { suggestion ->
            AISuggestionChip(
                suggestion = suggestion,
                onClick = { onSuggestionClick(suggestion) }
            )
        }
    }
}

@Composable
private fun AISuggestionChip(
    suggestion: AISuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    var isPressed by remember { mutableStateOf(false) }
    
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "chipScale"
    )
    
    val confidenceColor = when {
        suggestion.confidence >= 0.8f -> ModernColors.Success
        suggestion.confidence >= 0.6f -> ModernColors.Warning
        else -> ModernColors.Error
    }
    
    Surface(
        modifier = modifier
            .scale(scale)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            },
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 2.dp,
        shadowElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = suggestion.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            
            Text(
                text = suggestion.title,
                style = AITypography.aiSuggestion,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // Confidence indicator
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(confidenceColor, CircleShape)
            )
        }
    }
}

@Composable
fun AIThinkingIndicator(
    state: AIThinkingState,
    message: String = "",
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = state != AIThinkingState.IDLE,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically(),
        modifier = modifier
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 4.dp,
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (state) {
                    AIThinkingState.THINKING -> {
                        AIThinkingAnimation()
                        Text(
                            text = message.ifEmpty { "AI is thinking..." },
                            style = AITypography.aiThinking,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AIThinkingState.PROCESSING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = message.ifEmpty { "Processing..." },
                            style = AITypography.aiThinking,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    AIThinkingState.COMPLETE -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = ModernColors.Success,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = message.ifEmpty { "Complete!" },
                            style = AITypography.aiThinking,
                            color = ModernColors.Success
                        )
                    }
                    AIThinkingState.ERROR -> {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = ModernColors.Error,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = message.ifEmpty { "Something went wrong" },
                            style = AITypography.aiThinking,
                            color = ModernColors.Error
                        )
                    }
                    else -> {}
                }
            }
        }
    }
}

@Composable
private fun AIThinkingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking")
    val dots = listOf("●", "●", "●")
    
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        dots.forEachIndexed { index, dot ->
            val alpha by infiniteTransition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600),
                    repeatMode = RepeatMode.Reverse,
                    initialStartOffset = StartOffset(index * 200)
                ),
                label = "dotAlpha$index"
            )
            
            Text(
                text = dot,
                color = MaterialTheme.colorScheme.primary.copy(alpha = alpha),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun AIFloatingAssistant(
    isVisible: Boolean,
    onToggle: () -> Unit,
    onQuickAction: (AIActionType) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        // Quick actions panel
        AnimatedVisibility(
            visible = isVisible,
            enter = fadeIn() + slideInVertically { it / 2 },
            exit = fadeOut() + slideOutVertically { it / 2 }
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "AI Quick Actions",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    val quickActions = listOf(
                        AIActionType.SUMMARIZE to Icons.Default.Summarize,
                        AIActionType.EXPAND to Icons.Default.ExpandMore,
                        AIActionType.TRANSLATE to Icons.Default.Translate,
                        AIActionType.ORGANIZE to Icons.Default.Category
                    )
                    
                    quickActions.forEach { (actionType, icon) ->
                        AIQuickActionButton(
                            actionType = actionType,
                            icon = icon,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onQuickAction(actionType)
                            }
                        )
                    }
                }
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onToggle()
            },
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(16.dp)
        ) {
            AnimatedContent(
                targetState = isVisible,
                transitionSpec = {
                    fadeIn() + scaleIn() togetherWith fadeOut() + scaleOut()
                },
                label = "fabIcon"
            ) { visible ->
                Icon(
                    imageVector = if (visible) Icons.Default.Close else Icons.Default.Psychology,
                    contentDescription = if (visible) "Close AI Assistant" else "Open AI Assistant"
                )
            }
        }
    }
}

@Composable
private fun AIQuickActionButton(
    actionType: AIActionType,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Text(
                text = actionType.name.lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun AIConfidenceIndicator(
    confidence: Float,
    modifier: Modifier = Modifier
) {
    val confidenceColor = when {
        confidence >= 0.8f -> ModernColors.Success
        confidence >= 0.6f -> ModernColors.Warning
        else -> ModernColors.Error
    }
    
    val confidenceText = when {
        confidence >= 0.8f -> "High confidence"
        confidence >= 0.6f -> "Medium confidence"
        else -> "Low confidence"
    }
    
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LinearProgressIndicator(
            progress = { confidence },
            modifier = Modifier
                .width(60.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = confidenceColor,
            trackColor = confidenceColor.copy(alpha = 0.3f)
        )
        
        Text(
            text = confidenceText,
            style = AITypography.aiConfidence,
            color = confidenceColor
        )
    }
}
