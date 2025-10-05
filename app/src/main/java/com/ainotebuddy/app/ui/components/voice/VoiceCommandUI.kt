package com.ainotebuddy.app.ui.components.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Help
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ainotebuddy.app.R
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.ainotebuddy.app.ui.viewmodel.VoiceCommandUiState
import kotlinx.coroutines.delay
import kotlin.math.max

/**
 * Main voice command UI component that shows a floating action button
 * and handles the voice command interface
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun VoiceCommandUI(
    modifier: Modifier = Modifier,
    uiState: VoiceCommandUiState,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit,
    onConfirmAction: (() -> Unit)? = null,
    onCancelAction: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var showHelp by remember { mutableStateOf(false) }
    
    // Show help dialog if needed
    if (showHelp) {
        VoiceCommandHelpDialog(
            onDismiss = { showHelp = false },
            onOpenSettings = {
                showHelp = false
                onOpenSettings()
            }
        )
    }
    
    // Show appropriate UI based on state
    when (uiState) {
        is VoiceCommandUiState.Idle -> {
            // Show floating mic button when idle
            Box(
                modifier = modifier
                    .padding(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(
                    onClick = onStartListening,
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = stringResource(R.string.voice_command_start),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                // Help button
                IconButton(
                    onClick = { showHelp = true },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Help,
                        contentDescription = stringResource(R.string.help),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        is VoiceCommandUiState.Listening -> {
            VoiceCommandDialog(
                title = stringResource(R.string.listening),
                message = stringResource(R.string.speak_now),
                icon = Icons.Default.Mic,
                iconTint = MaterialTheme.colorScheme.primary,
                showProgress = true,
                onDismiss = onStopListening,
                onAction = onStopListening,
                actionText = stringResource(R.string.stop),
                showCancel = true
            )
        }
        
        is VoiceCommandUiState.Recognizing -> {
            VoiceCommandDialog(
                title = stringResource(R.string.recognizing),
                message = uiState.text,
                icon = Icons.Default.Mic,
                iconTint = MaterialTheme.colorScheme.primary,
                showProgress = true,
                progress = uiState.confidence,
                onDismiss = onStopListening,
                onAction = onStopListening,
                actionText = stringResource(R.string.stop),
                showCancel = true
            )
        }
        
        is VoiceCommandUiState.Processing -> {
            VoiceCommandDialog(
                title = stringResource(R.string.processing),
                message = uiState.message,
                icon = Icons.Default.Check,
                iconTint = MaterialTheme.colorScheme.primary,
                showProgress = true,
                onDismiss = onDismiss,
                showAction = false
            )
        }
        
        is VoiceCommandUiState.CommandExecuted -> {
            // Auto-dismiss after a delay
            LaunchedEffect(Unit) {
                delay(2000)
                onDismiss()
            }
            
            VoiceCommandDialog(
                title = uiState.command,
                message = uiState.result,
                icon = if (uiState.success) Icons.Default.Check else Icons.Default.Close,
                iconTint = if (uiState.success) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                showProgress = false,
                onDismiss = onDismiss,
                showAction = false
            )
        }
        
        is VoiceCommandUiState.Error -> {
            VoiceCommandDialog(
                title = stringResource(R.string.error),
                message = uiState.message,
                icon = Icons.Default.Close,
                iconTint = MaterialTheme.colorScheme.error,
                showProgress = false,
                onDismiss = onDismiss,
                actionText = stringResource(R.string.retry),
                onAction = onStartListening,
                showCancel = true
            )
        }
        
        is VoiceCommandUiState.ConfirmationRequired -> {
            VoiceCommandDialog(
                title = uiState.command,
                message = uiState.message,
                icon = Icons.Default.Check,
                iconTint = MaterialTheme.colorScheme.primary,
                showProgress = false,
                onDismiss = { onCancelAction?.invoke() ?: onDismiss() },
                actionText = stringResource(R.string.confirm),
                onAction = { onConfirmAction?.invoke() ?: onDismiss() },
                showCancel = true,
                cancelText = stringResource(R.string.cancel),
                onCancel = { onCancelAction?.invoke() ?: onDismiss() }
            )
        }
        
        is VoiceCommandUiState.PermissionRequired -> {
            VoiceCommandDialog(
                title = stringResource(R.string.permission_required),
                message = stringResource(R.string.mic_permission_required),
                icon = Icons.Default.Settings,
                iconTint = MaterialTheme.colorScheme.primary,
                showProgress = false,
                onDismiss = onDismiss,
                actionText = stringResource(R.string.open_settings),
                onAction = onOpenSettings,
                showCancel = true
            )
        }
        
        is VoiceCommandUiState.Help -> {
            VoiceCommandHelpDialog(
                onDismiss = onDismiss,
                onOpenSettings = onOpenSettings
            )
        }
        
        is VoiceCommandUiState.Navigation -> {
            // Handle navigation and auto-dismiss
            LaunchedEffect(Unit) {
                delay(500)
                onDismiss()
            }
            
            VoiceCommandDialog(
                title = stringResource(R.string.navigating),
                message = "${stringResource(R.string.going_to)} ${uiState.destination}",
                icon = Icons.Default.Check,
                iconTint = MaterialTheme.colorScheme.primary,
                showProgress = false,
                onDismiss = onDismiss,
                showAction = false
            )
        }
    }
}

/**
 * Dialog for displaying voice command UI
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceCommandDialog(
    title: String,
    message: String,
    icon: ImageVector,
    iconTint: Color,
    showProgress: Boolean,
    progress: Float = 0f,
    onDismiss: () -> Unit,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
    showAction: Boolean = true,
    showCancel: Boolean = false,
    cancelText: String = stringResource(R.string.cancel),
    onCancel: (() -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(iconTint.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(28.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Title
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Message
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Progress indicator
                if (showProgress) {
                    if (progress > 0f) {
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                        )
                        
                        Text(
                            text = "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    } else {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showCancel) {
                        TextButton(
                            onClick = { onCancel?.invoke() ?: onDismiss() },
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(text = cancelText.uppercase())
                        }
                    }
                    
                    if (showAction && actionText != null && onAction != null) {
                        Button(
                            onClick = onAction,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            )
                        ) {
                            Text(text = actionText.uppercase())
                        }
                    }
                }
            }
        }
    }
}

/**
 * Help dialog showing available voice commands
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceCommandHelpDialog(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = stringResource(R.string.voice_commands),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Command list
                VoiceCommandHelpItem(
                    command = "Create note [content]",
                    description = stringResource(R.string.create_note_help)
                )
                
                VoiceCommandHelpItem(
                    command = "Add to note [content]",
                    description = stringResource(R.string.add_to_note_help)
                )
                
                VoiceCommandHelpItem(
                    command = "Search for [query]",
                    description = stringResource(R.string.search_help)
                )
                
                VoiceCommandHelpItem(
                    command = "Open note [title]",
                    description = stringResource(R.string.open_note_help)
                )
                
                VoiceCommandHelpItem(
                    command = "Delete note [title]",
                    description = stringResource(R.string.delete_note_help)
                )
                
                VoiceCommandHelpItem(
                    command = "Set reminder [time] [content]",
                    description = stringResource(R.string.set_reminder_help)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Settings button
                Button(
                    onClick = {
                        onDismiss()
                        onOpenSettings()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.voice_settings))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Close button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(text = stringResource(R.string.got_it))
                }
            }
        }
    }
}

/**
 * Item in the voice command help list
 */
@Composable
private fun VoiceCommandHelpItem(
    command: String,
    description: String
) {
    Column(
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Text(
            text = command,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun VoiceCommandUIPreview() {
    AINoteBuddyTheme {
        Surface {
            Column {
                // Idle state
                VoiceCommandUI(
                    uiState = VoiceCommandUiState.Idle,
                    onStartListening = {},
                    onStopListening = {},
                    onDismiss = {},
                    onOpenSettings = {}
                )
                
                // Listening state
                VoiceCommandUI(
                    uiState = VoiceCommandUiState.Listening,
                    onStartListening = {},
                    onStopListening = {},
                    onDismiss = {},
                    onOpenSettings = {}
                )
                
                // Recognizing state
                VoiceCommandUI(
                    uiState = VoiceCommandUiState.Recognizing("This is a test command", 0.75f),
                    onStartListening = {},
                    onStopListening = {},
                    onDismiss = {},
                    onOpenSettings = {}
                )
                
                // Command executed state
                VoiceCommandUI(
                    uiState = VoiceCommandUiState.CommandExecuted(
                        command = "Note Created",
                        result = "Successfully created a new note",
                        success = true
                    ),
                    onStartListening = {},
                    onStopListening = {},
                    onDismiss = {},
                    onOpenSettings = {}
                )
                
                // Error state
                VoiceCommandUI(
                    uiState = VoiceCommandUiState.Error("Could not recognize speech"),
                    onStartListening = {},
                    onStopListening = {},
                    onDismiss = {},
                    onOpenSettings = {}
                )
                
                // Confirmation required state
                VoiceCommandUI(
                    uiState = VoiceCommandUiState.ConfirmationRequired(
                        command = "Delete Note",
                        message = "Are you sure you want to delete 'Shopping List'?",
                        positiveAction = {},
                        negativeAction = {}
                    ),
                    onStartListening = {},
                    onStopListening = {},
                    onDismiss = {},
                    onOpenSettings = {},
                    onConfirmAction = {},
                    onCancelAction = {}
                )
                
                // Help dialog
                VoiceCommandUI(
                    uiState = VoiceCommandUiState.Help,
                    onStartListening = {},
                    onStopListening = {},
                    onDismiss = {},
                    onOpenSettings = {}
                )
            }
        }
    }
}
