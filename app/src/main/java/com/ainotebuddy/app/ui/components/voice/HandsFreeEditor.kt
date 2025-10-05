package com.ainotebuddy.app.ui.components.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.KeyboardVoice
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ainotebuddy.app.R
import com.ainotebuddy.app.voice.model.VoiceCommand
import com.ainotebuddy.app.voice.model.VoiceNavigationFeedback
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

/**
 * Composable for hands-free note editing with voice commands
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HandsFreeEditor(
    modifier: Modifier = Modifier,
    currentContent: String,
    onContentUpdate: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    isListening: Boolean = false,
    isProcessing: Boolean = false,
    feedback: VoiceNavigationFeedback = VoiceNavigationFeedback.Idle,
    onStartListening: () -> Unit = {},
    onStopListening: () -> Unit = {},
    onCommandRecognized: (VoiceCommand) -> Unit = {}
) {
    var showFeedback by remember { mutableStateOf(false) }
    var feedbackMessage by remember { mutableStateOf("") }
    var feedbackType by remember { mutableStateOf(FeedbackType.INFO) }
    
    // Show feedback when it changes
    LaunchedEffect(feedback) {
        when (feedback) {
            is VoiceNavigationFeedback.Success -> {
                feedbackMessage = feedback.message
                feedbackType = FeedbackType.SUCCESS
                showFeedback = true
                delay(2000)
                showFeedback = false
            }
            is VoiceNavigationFeedback.Error -> {
                feedbackMessage = feedback.message
                feedbackType = FeedbackType.ERROR
                showFeedback = true
                delay(3000)
                showFeedback = false
            }
            is VoiceNavigationFeedback.Processing -> {
                feedbackMessage = "Processing..."
                feedbackType = FeedbackType.INFO
                showFeedback = true
            }
            is VoiceNavigationFeedback.Listening -> {
                feedbackMessage = "Listening..."
                feedbackType = FeedbackType.INFO
                showFeedback = true
            }
            else -> {
                showFeedback = false
            }
        }
    }
    
    // Handle confirmation dialog
    if (feedback is VoiceNavigationFeedback.ConfirmationRequired) {
        val confirmFeedback = feedback as VoiceNavigationFeedback.ConfirmationRequired
        ConfirmationDialog(
            message = confirmFeedback.message,
            onConfirm = {
                confirmFeedback.onConfirm()
                showFeedback = false
            },
            onDismiss = {
                confirmFeedback.onDismiss()
                showFeedback = false
            }
        )
    }
    
    // Main content
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Content display
        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = currentContent.ifEmpty { "Speak to add content..." },
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
        
        // Controls
        VoiceControlBar(
            isListening = isListening,
            isProcessing = isProcessing,
            onStartListening = onStartListening,
            onStopListening = onStopListening,
            onSave = onSave,
            onCancel = onCancel
        )
        
        // Feedback indicator
        if (showFeedback) {
            FeedbackIndicator(
                message = feedbackMessage,
                type = feedbackType,
                modifier = Modifier.padding(16.dp)
            )
        }
    }
}

/**
 * Voice control bar with action buttons
 */
@Composable
private fun VoiceControlBar(
    isListening: Boolean,
    isProcessing: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Record button
            IconButton(
                onClick = { if (isListening) onStopListening() else onStartListening() },
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = if (isListening) Icons.Default.Check else Icons.Default.Edit,
                    contentDescription = if (isListening) "Stop" else "Start",
                    tint = if (isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
            }
            
            // Progress indicator
            if (isProcessing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .weight(3f)
                        .height(4.dp)
                        .padding(horizontal = 8.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                Spacer(modifier = Modifier.weight(3f))
            }
            
            // Save button
            IconButton(
                onClick = onSave,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Save",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            // Cancel button
            IconButton(
                onClick = onCancel,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cancel",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

/**
 * Feedback indicator for voice commands
 */
@Composable
private fun FeedbackIndicator(
    message: String,
    type: FeedbackType,
    modifier: Modifier = Modifier
) {
    val backgroundColor = when (type) {
        FeedbackType.SUCCESS -> MaterialTheme.colorScheme.primaryContainer
        FeedbackType.ERROR -> MaterialTheme.colorScheme.errorContainer
        FeedbackType.INFO -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val textColor = when (type) {
        FeedbackType.SUCCESS -> MaterialTheme.colorScheme.onPrimaryContainer
        FeedbackType.ERROR -> MaterialTheme.colorScheme.onErrorContainer
        FeedbackType.INFO -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Text(
            text = message,
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Confirmation dialog for destructive actions
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfirmationDialog(
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Card(
            modifier = modifier,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Confirm Action",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    TextButton(
                        onClick = onConfirm,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Confirm")
                    }
                }
            }
        }
    }
}

/**
 * Type of feedback to display
 */
private enum class FeedbackType {
    SUCCESS, ERROR, INFO
}

@Preview(showBackground = true)
@Composable
private fun HandsFreeEditorPreview() {
    MaterialTheme {
        Surface {
            HandsFreeEditor(
                currentContent = "This is a sample note created with voice commands.",
                onContentUpdate = {},
                onSave = {},
                onCancel = {},
                isListening = true,
                isProcessing = false,
                feedback = VoiceNavigationFeedback.Listening
            )
        }
    }
}
