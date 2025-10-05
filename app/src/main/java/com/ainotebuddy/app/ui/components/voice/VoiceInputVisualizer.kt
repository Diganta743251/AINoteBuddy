package com.ainotebuddy.app.ui.components.voice

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ainotebuddy.app.R
import com.ainotebuddy.app.voice.model.VoiceNavigationFeedback
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Visual feedback component for voice input
 */
@Composable
fun VoiceInputVisualizer(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    volume: Float = 0f, // 0f to 1f
    feedback: VoiceNavigationFeedback = VoiceNavigationFeedback.Idle,
    showTextPrompt: Boolean = true,
    onDismiss: () -> Unit = {}
) {
    val animatedVolume = remember { Animatable(0f) }
    val primaryColor = MaterialTheme.colorScheme.primary
    val isActive = isListening || isProcessing
    // Read Material theme colors in a composable context (outside Canvas draw lambdas)
    val colorPrimary = MaterialTheme.colorScheme.primary
    val colorSecondary = MaterialTheme.colorScheme.secondary
    val colorSurfaceVariant = MaterialTheme.colorScheme.surfaceVariant
    
    // Animate volume changes
    LaunchedEffect(volume, isActive) {
        if (isActive) {
            animatedVolume.animateTo(
                targetValue = volume,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )
        } else {
            animatedVolume.animateTo(0f)
        }
    }
    
    // Pulsing animation when processing
    val pulse = remember { Animatable(0f) }
    LaunchedEffect(isProcessing) {
        if (isProcessing) {
            pulse.animateTo(
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                )
            )
        } else {
            pulse.snapTo(0f)
        }
    }
    
    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        val size = minOf(constraints.maxWidth, constraints.maxHeight).toFloat()
        val centerX = constraints.maxWidth / 2f
        val centerY = constraints.maxHeight / 2f
        
        // Background circle
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val radius = size * 0.4f
            val activeColor = colorPrimary
            val inactiveColor = colorSurfaceVariant
            
            // Draw the base circle
            drawCircle(
                color = if (isActive) activeColor.copy(alpha = 0.1f) 
                       else inactiveColor.copy(alpha = 0.1f),
                radius = radius,
                center = Offset(centerX, centerY),
                style = Stroke(width = 2.dp.toPx())
            )
            
            // Draw volume indicator
            if (isActive) {
                val volumeRadius = radius * 0.6f + (radius * 0.4f * animatedVolume.value)
                drawCircle(
                    color = activeColor.copy(alpha = 0.2f),
                    radius = volumeRadius,
                    center = Offset(centerX, centerY)
                )
                
                // Draw sound waves
                val waveCount = 8
                val waveLength = 360f / waveCount
                val waveAmplitude = 10f * animatedVolume.value
                
                for (i in 0 until waveCount) {
                    val angle = Math.toRadians((i * waveLength).toDouble()).toFloat()
                    val waveRadius = radius + waveAmplitude * sin(angle * 5 + System.currentTimeMillis() / 500f)
                    val x = centerX + waveRadius * cos(angle)
                    val y = centerY + waveRadius * sin(angle)
                    
                    drawCircle(
                        color = activeColor,
                        radius = 4f,
                        center = Offset(x, y),
                        alpha = 0.6f
                    )
                }
            }
            
            // Draw processing animation
            if (isProcessing) {
                val processingRadius = radius * (0.8f + 0.2f * pulse.value)
                drawCircle(
                    color = colorSecondary.copy(alpha = 0.3f * pulse.value),
                    radius = processingRadius,
                    center = Offset(centerX, centerY)
                )
                
                // Draw rotating dots
                val dotCount = 8
                val dotRadius = 4f
                val dotDistance = radius * 0.7f
                val time = System.currentTimeMillis() / 1000f
                
                for (i in 0 until dotCount) {
                    val angle = (i * (2 * PI / dotCount) + time * 2).toFloat()
                    val x = centerX + dotDistance * cos(angle)
                    val y = centerY + dotDistance * sin(angle)
                    val alpha = 0.3f + 0.7f * ((sin(angle * 2 + time * 4) + 1) / 2)
                    
                    drawCircle(
                    color = colorSecondary.copy(alpha = alpha),
                    radius = dotRadius,
                    center = Offset(x, y)
                )
            }
            }
        }
        
        // Center icon
        Box(
            modifier = Modifier
                .size((size * 0.5f).dp)
                .clip(CircleShape)
                .background(
                    when {
                        isProcessing -> MaterialTheme.colorScheme.secondaryContainer
                        isListening -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isProcessing) {
                // Pulsing dots animation
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    val dotSize = 8.dp
                    val dotSpacing = 4.dp
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(3) { index ->
                            val delay = index * 200L
                            val scale = remember { Animatable(0.5f) }
                            
                            LaunchedEffect(isProcessing) {
                                if (isProcessing) {
                                    while (true) {
                                        delay(delay)
                                        scale.animateTo(1f, tween(200))
                                        scale.animateTo(0.5f, tween(200))
                                        delay(600 - delay)
                                    }
                                } else {
                                    scale.snapTo(0.5f)
                                }
                            }
                            
                            Box(
                                modifier = Modifier
                                    .size(dotSize * scale.value)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSecondaryContainer)
                            )
                            
                            if (index < 2) {
                                Spacer(modifier = Modifier.width(dotSpacing))
                            }
                        }
                    }
                }
            } else {
                Icon(
                    imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicOff,
                    contentDescription = if (isListening) "Listening" else "Microphone off",
                    tint = if (isListening) MaterialTheme.colorScheme.onPrimaryContainer 
                          else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        
        // Text prompt
        if (showTextPrompt) {
            val promptText = when {
                isProcessing -> "Processing..."
                isListening -> "Listening..."
                feedback is VoiceNavigationFeedback.Error -> feedback.message
                feedback is VoiceNavigationFeedback.Success -> feedback.message
                else -> "Tap to speak"
            }
            
            val promptColor = when {
                isProcessing -> MaterialTheme.colorScheme.onSecondaryContainer
                isListening -> MaterialTheme.colorScheme.onPrimaryContainer
                feedback is VoiceNavigationFeedback.Error -> MaterialTheme.colorScheme.error
                feedback is VoiceNavigationFeedback.Success -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            
            Text(
                text = promptText,
                color = promptColor,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )
        }
        
        // Close button when not listening
        if (!isListening && !isProcessing) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * A compact version of the voice input visualizer for use in toolbars or other constrained spaces
 */
@Composable
fun CompactVoiceInputVisualizer(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    volume: Float = 0f,
    onClick: () -> Unit = {}
) {
    val animatedVolume = remember { Animatable(0f) }
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Animate volume changes
    LaunchedEffect(volume, isListening) {
        if (isListening) {
            animatedVolume.animateTo(
                targetValue = volume,
                animationSpec = tween(durationMillis = 100, easing = LinearEasing)
            )
        } else {
            animatedVolume.animateTo(0f)
        }
    }
    
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(
                when {
                    isProcessing -> MaterialTheme.colorScheme.secondaryContainer
                    isListening -> MaterialTheme.colorScheme.primaryContainer
                    else -> MaterialTheme.colorScheme.surfaceVariant
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (isProcessing) {
            // Pulsing animation
            val pulse = remember { Animatable(0f) }
            
            LaunchedEffect(isProcessing) {
                if (isProcessing) {
                    pulse.animateTo(
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = RepeatMode.Reverse
                        )
                    )
                } else {
                    pulse.snapTo(0f)
                }
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize(0.6f + 0.4f * pulse.value)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f * pulse.value))
            )
        }
        
        Icon(
            imageVector = when {
                isProcessing -> Icons.Default.Mic
                isListening -> Icons.Default.Mic
                else -> Icons.Default.MicOff
            },
            contentDescription = if (isListening) "Listening" else "Microphone",
            tint = when {
                isProcessing -> MaterialTheme.colorScheme.onSecondaryContainer
                isListening -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(20.dp)
        )
        
        // Volume indicator
        if (isListening) {
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val radius = size.minDimension * 0.4f * (1 + animatedVolume.value * 0.5f)
                drawCircle(
                    color = primaryColor.copy(alpha = 0.2f),
                    radius = radius,
                    center = Offset(size.width / 2, size.height / 2)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceInputVisualizerPreview() {
    MaterialTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Idle state
            VoiceInputVisualizer(
                modifier = Modifier.size(200.dp),
                isListening = false,
                isProcessing = false
            )
            
            // Listening state
            VoiceInputVisualizer(
                modifier = Modifier.size(200.dp),
                isListening = true,
                volume = 0.7f
            )
            
            // Processing state
            VoiceInputVisualizer(
                modifier = Modifier.size(200.dp),
                isProcessing = true
            )
            
            // Compact versions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CompactVoiceInputVisualizer(isListening = false)
                CompactVoiceInputVisualizer(isListening = true, volume = 0.5f)
                CompactVoiceInputVisualizer(isProcessing = true)
            }
        }
    }
}
