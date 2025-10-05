package com.ainotebuddy.app.ui.components.ai

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import com.ainotebuddy.app.R
import com.ainotebuddy.app.ai.VoiceCommand
import com.ainotebuddy.app.ai.VoiceCommandResult
import com.ainotebuddy.app.ui.theme.LocalSpacing

/**
 * A floating action button that shows voice command UI when clicked
 */
@Composable
fun VoiceCommandFAB(
    isListening: Boolean,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    modifier: Modifier = Modifier,
    commands: List<VoiceCommand> = emptyList(),
    onCommandClick: (VoiceCommand) -> Unit = {},
    result: VoiceCommandResult? = null,
    onDismissResult: () -> Unit = {}
) {
    val spacing = LocalSpacing.current
    var showCommands by remember { mutableStateOf(false) }
    
    Box(
        modifier = modifier
    ) {
        // Voice command list
        AnimatedVisibility(
            visible = showCommands,
            enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
            exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                VoiceCommandsList(
                    commands = commands,
                    onCommandClick = {
                        onCommandClick(it)
                        showCommands = false
                    },
                    modifier = Modifier
                        .padding(bottom = 80.dp, end = 16.dp)
                        .fillMaxWidth(0.8f)
                        .align(Alignment.BottomEnd)
                )
            }
        }
        
        // Main FAB
        FloatingActionButton(
            onClick = {
                if (isListening) {
                    onStopListening()
                } else {
                    onStartListening()
                }
            },
            containerColor = if (isListening) MaterialTheme.colorScheme.errorContainer
            else MaterialTheme.colorScheme.primaryContainer,
            contentColor = if (isListening) MaterialTheme.colorScheme.onErrorContainer
            else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(56.dp)
                .align(Alignment.BottomEnd)
        ) {
            val iconRotation by animateFloatAsState(if (isListening) 90f else 0f)
            Icon(
                imageVector = if (isListening) Icons.Default.MicOff else Icons.Default.Mic,
                contentDescription = if (isListening) "Stop listening" else "Start voice command",
                modifier = Modifier.rotate(iconRotation)
            )
        }
        
        // Commands FAB
        AnimatedVisibility(
            visible = !isListening,
            enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
            exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                FloatingActionButton(
                    onClick = { showCommands = !showCommands },
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.BottomEnd)
                        .padding(bottom = 0.dp, end = 64.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardVoice,
                        contentDescription = "Voice commands",
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
        
        // Voice command result
        result?.let { commandResult ->
            val isError = !commandResult.success
            val backgroundColor = if (isError) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.primaryContainer
            }
            val contentColor = if (isError) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onPrimaryContainer
            }
            
            Snackbar(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
                    .fillMaxWidth(0.9f),
                containerColor = backgroundColor,
                contentColor = contentColor,
                action = {
                    TextButton(onClick = onDismissResult) {
                        Text("Dismiss")
                    }
                }
            ) {
                Column {
                    Text(
                        text = if (isError) "Error processing command" 
                              else "Command executed: ${commandResult.commandType}",
                        fontWeight = FontWeight.Bold
                    )
                    if (commandResult.message != null) {
                        Text(commandResult.message)
                    }
                }
            }
        }
    }
}

/**
 * Displays a list of available voice commands
 */
@Composable
private fun VoiceCommandsList(
    commands: List<VoiceCommand>,
    onCommandClick: (VoiceCommand) -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier.padding(spacing.medium)
        ) {
            Text(
                text = "Available Commands",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = spacing.small)
            )
            
            if (commands.isEmpty()) {
                Text(
                    text = "No voice commands available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = spacing.medium)
                )
            } else {
                commands.forEach { command ->
                    VoiceCommandItem(
                        command = command,
                        onClick = { onCommandClick(command) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (command != commands.last()) {
                        Divider(
                            modifier = Modifier.padding(vertical = spacing.small),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single voice command item in the list
 */
@Composable
private fun VoiceCommandItem(
    command: VoiceCommand,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val spacing = LocalSpacing.current
    
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = spacing.small)
        ) {
            Text(
                text = command.command,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.height(spacing.extraSmall))
            
            Text(
                text = command.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            if (command.example.isNotBlank()) {
                Spacer(modifier = Modifier.height(spacing.extraSmall))
                
                Text(
                    text = "Example: ${command.example}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Preview(device = Devices.PIXEL_4)
@Composable
private fun VoiceCommandUIPreview() {
    val sampleCommands = listOf(
        VoiceCommand(
            id = "create_note",
            command = "Create a new note about [topic]",
            description = "Creates a new note with the given topic",
            example = "Create a new note about team meeting"
        ),
        VoiceCommand(
            id = "search_notes",
            command = "Find notes about [query]",
            description = "Searches for notes containing the query",
            example = "Find notes about project deadline"
        ),
        VoiceCommand(
            id = "set_reminder",
            command = "Remind me to [task] at [time]",
            description = "Sets a reminder for the specified task and time",
            example = "Remind me to call John at 3pm"
        )
    )
    
    MaterialTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            VoiceCommandFAB(
                isListening = false,
                onStartListening = {},
                onStopListening = {},
                commands = sampleCommands,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
    }
}
