package com.ainotebuddy.app.ui.components.voice

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.voice.model.VoiceNavigationFeedback

/**
 * A combined floating action button that toggles between AI and Voice modes
 */
@Composable
fun VoiceAIComboButton(
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    showAI: Boolean = false,
    feedback: VoiceNavigationFeedback = VoiceNavigationFeedback.Idle,
    onVoiceClick: () -> Unit = {},
    onAIClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    
    // Animation specs
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonRotation"
    )
    
    val buttonScale by animateFloatAsState(
        targetValue = if (isListening || isProcessing) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "buttonScale"
    )
    
    Box(
        modifier = modifier
            .padding(16.dp)
    ) {
        // Voice mode button (always visible when active)
        if (isListening || isProcessing) {
            FloatingActionButton(
                onClick = { onVoiceClick() },
                containerColor = when {
                    isProcessing -> MaterialTheme.colorScheme.secondaryContainer
                    isListening -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primaryContainer
                },
                contentColor = when {
                    isProcessing -> MaterialTheme.colorScheme.onSecondaryContainer
                    isListening -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onPrimaryContainer
                },
                modifier = Modifier
                    .size(56.dp)
                    .scale(buttonScale)
                    .align(Alignment.BottomEnd)
            ) {
                Icon(
                    imageVector = when {
                        isProcessing -> Icons.Default.Stop
                        isListening -> Icons.Default.Mic
                        else -> Icons.Default.MicOff
                    },
                    contentDescription = if (isListening) "Stop listening" else "Start listening",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        
        // Main FAB that toggles between AI and Voice modes
        FloatingActionButton(
            onClick = { 
                if (isListening || isProcessing) {
                    showHelp = true
                } else {
                    expanded = !expanded 
                }
            },
            containerColor = when {
                isListening || isProcessing -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.9f)
                showAI -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.primaryContainer
            },
            contentColor = when {
                isListening || isProcessing -> MaterialTheme.colorScheme.onPrimaryContainer
                showAI -> MaterialTheme.colorScheme.onSecondaryContainer
                else -> MaterialTheme.colorScheme.onPrimaryContainer
            },
            modifier = Modifier
                .size(if (expanded) 52.dp else 56.dp)
                .scale(buttonScale)
                .align(Alignment.BottomEnd)
                .offset(
                    x = if (isListening || isProcessing) (-16).dp else 0.dp,
                    y = 0.dp
                )
        ) {
            Icon(
                imageVector = if (showAI) Icons.Default.Psychology else Icons.Default.KeyboardVoice,
                contentDescription = if (showAI) "AI Assistant" else "Voice Commands",
                modifier = Modifier.rotate(rotation)
            )
        }
        
        // Expanded menu
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(y = (-72).dp)
            ) {
                // Voice command option
                Surface(
                    onClick = {
                        onVoiceClick()
                        expanded = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Voice Commands",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.KeyboardVoice,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // AI Assistant option
                Surface(
                    onClick = {
                        onAIClick()
                        expanded = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "AI Assistant",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Icon(
                            imageVector = Icons.Default.Psychology,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
        
        // Visual feedback for voice input
        if (isListening || isProcessing) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(72.dp)
                    .offset(
                        x = if (isListening || isProcessing) (-16).dp else 0.dp,
                        y = 0.dp
                    )
                    .clip(CircleShape)
                    .background(
                        when {
                            isProcessing -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)
                            isListening -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            else -> Color.Transparent
                        }
                    )
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceAIComboButtonPreview() {
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .padding(16.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            var isListening by remember { mutableStateOf(false) }
            var showAI by remember { mutableStateOf(false) }
            
            VoiceAIComboButton(
                isListening = isListening,
                isProcessing = false,
                showAI = showAI,
                onVoiceClick = { isListening = !isListening },
                onAIClick = { showAI = !showAI }
            )
        }
    }
}
