package com.ainotebuddy.app.ui.components.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import com.ainotebuddy.app.R
import com.ainotebuddy.app.voice.model.VoiceNavigationFeedback
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * A floating action button that shows voice command status and provides access to voice features
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingVoiceButton(
    modifier: Modifier = Modifier,
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    feedback: VoiceNavigationFeedback = VoiceNavigationFeedback.Idle,
    onStartListening: () -> Unit = {},
    onStopListening: () -> Unit = {},
    onDismiss: () -> Unit = {},
    showFeedback: Boolean = true,
    position: FloatingButtonPosition = FloatingButtonPosition.BOTTOM_END
) {
    var showTooltip by remember { mutableStateOf(false) }
    var showFeedbackCard by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackType by remember { mutableStateOf(VoiceFeedbackType.INFO) }
    
    // Animation states
    val rotation by animateFloatAsState(
        targetValue = if (isListening) 15f else 0f,
        label = "buttonRotation"
    )
    
    // Handle feedback changes
    LaunchedEffect(feedback) {
        when (feedback) {
            is VoiceNavigationFeedback.Success -> {
                feedbackMessage = feedback.message
                feedbackType = VoiceFeedbackType.SUCCESS
                showFeedbackCard = true
                delay(2000)
                showFeedbackCard = false
            }
            is VoiceNavigationFeedback.Error -> {
                feedbackMessage = feedback.message
                feedbackType = VoiceFeedbackType.ERROR
                showFeedbackCard = true
                delay(3000)
                showFeedbackCard = false
            }
            is VoiceNavigationFeedback.Processing -> {
                feedbackMessage = "Processing..."
                feedbackType = VoiceFeedbackType.INFO
                showFeedbackCard = true
            }
            is VoiceNavigationFeedback.Listening -> {
                feedbackMessage = "Listening..."
                feedbackType = VoiceFeedbackType.INFO
                showFeedbackCard = true
            }
            else -> {
                showFeedbackCard = false
            }
        }
    }
    
    // Position the button based on the specified position
    val buttonModifier = when (position) {
        FloatingButtonPosition.TOP_START -> Modifier
            .padding(start = 16.dp, top = 16.dp)
            .then(modifier)
        FloatingButtonPosition.TOP_END -> Modifier
            .padding(end = 16.dp, top = 16.dp)
            .then(modifier)
        FloatingButtonPosition.BOTTOM_START -> Modifier
            .padding(start = 16.dp, bottom = 16.dp)
            .then(modifier)
        FloatingButtonPosition.BOTTOM_END -> Modifier
            .padding(end = 16.dp, bottom = 16.dp)
            .then(modifier)
    }
    
    Box(
        modifier = buttonModifier,
        contentAlignment = when (position) {
            FloatingButtonPosition.TOP_START -> Alignment.TopStart
            FloatingButtonPosition.TOP_END -> Alignment.TopEnd
            FloatingButtonPosition.BOTTOM_START -> Alignment.BottomStart
            FloatingButtonPosition.BOTTOM_END -> Alignment.BottomEnd
        }
    ) {
        Column(
            horizontalAlignment = when (position) {
                FloatingButtonPosition.TOP_START, FloatingButtonPosition.BOTTOM_START -> 
                    Alignment.Start
                else -> Alignment.End
            },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Feedback card
            if (showFeedback && showFeedbackCard) {
                AnimatedVisibility(
                    visible = showFeedbackCard,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    FeedbackCard(
                        message = feedbackMessage,
                        type = feedbackType,
                        onDismiss = { showFeedbackCard = false },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
            
            // Voice command button
            FloatingActionButton(
                onClick = {
                    if (isListening) {
                        onStopListening()
                    } else {
                        onStartListening()
                    }
                },
                containerColor = when {
                    isProcessing -> MaterialTheme.colorScheme.secondaryContainer
                    isListening -> MaterialTheme.colorScheme.errorContainer
                    else -> MaterialTheme.colorScheme.primary
                },
                contentColor = when {
                    isProcessing -> MaterialTheme.colorScheme.onSecondaryContainer
                    isListening -> MaterialTheme.colorScheme.onErrorContainer
                    else -> MaterialTheme.colorScheme.onPrimary
                },
                modifier = Modifier
                    .size(if (isListening || isProcessing) 64.dp else 56.dp)
                    .rotate(rotation)
            ) {
                when {
                    isProcessing -> {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = stringResource(R.string.processing),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    isListening -> {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = stringResource(R.string.stop_listening),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    else -> {
                        Icon(
                            imageVector = Icons.Default.KeyboardVoice,
                            contentDescription = stringResource(R.string.start_listening),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
            
            // Tooltip
            if (showTooltip && !isListening) {
                AnimatedVisibility(
                    visible = showTooltip,
                    enter = fadeIn() + slideInVertically { it / 2 },
                    exit = fadeOut() + slideOutVertically { it / 2 }
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.tap_to_speak),
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
        
        // Show a small indicator when listening
        if (isListening) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            )
        }
    }
    
    // Show tooltip on first appearance
    LaunchedEffect(Unit) {
        delay(1000)
        showTooltip = true
        delay(3000)
        showTooltip = false
    }
}

/**
 * Feedback card for showing messages to the user
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FeedbackCard(
        message: String,
        type: VoiceFeedbackType,
        onDismiss: () -> Unit,
        modifier: Modifier = Modifier
    ) {
    val backgroundColor = when (type) {
        VoiceFeedbackType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        VoiceFeedbackType.ERROR -> MaterialTheme.colorScheme.errorContainer
        VoiceFeedbackType.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when (type) {
        VoiceFeedbackType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        VoiceFeedbackType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        VoiceFeedbackType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = onDismiss,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = message,
                color = contentColor,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.dismiss),
                    tint = contentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/**
 * Type of feedback to show
 */
private enum class VoiceFeedbackType {
    SUCCESS, ERROR, INFO
}

/**
 * Position of the floating button
 */
enum class FloatingButtonPosition {
    TOP_START, TOP_END, BOTTOM_START, BOTTOM_END
}

@Preview(showBackground = true)
@Composable
private fun FloatingVoiceButtonPreview() {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize()) {
            // Idle state
            FloatingVoiceButton(
                position = FloatingButtonPosition.BOTTOM_END,
                modifier = Modifier.padding(16.dp)
            )
            
            // Listening state
            FloatingVoiceButton(
                isListening = true,
                position = FloatingButtonPosition.BOTTOM_START,
                modifier = Modifier.padding(16.dp)
            )
            
            // Processing state
            FloatingVoiceButton(
                isProcessing = true,
                position = FloatingButtonPosition.TOP_END,
                modifier = Modifier.padding(16.dp)
            )
            
            // With feedback
            FloatingVoiceButton(
                feedback = VoiceNavigationFeedback.Success("Note saved"),
                position = FloatingButtonPosition.TOP_START,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}
