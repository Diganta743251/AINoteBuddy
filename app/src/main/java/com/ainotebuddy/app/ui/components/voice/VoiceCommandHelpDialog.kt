package com.ainotebuddy.app.ui.components.voice

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ainotebuddy.app.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun VoiceCommandHelpDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    commands: List<VoiceCommand> = defaultVoiceCommands()
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AnimatedVisibility(
            visible = true,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut()
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraLarge,
                tonalElevation = 8.dp,
                modifier = modifier
                    .width(IntrinsicSize.Max)
                    .heightIn(max = 600.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    // Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Voice Commands",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close"
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Command categories
                    val categories = commands.groupBy { it.category }
                    
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        categories.forEach { (category, categoryCommands) ->
                            item {
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(categoryCommands) { command ->
                                CommandItem(command = command)
                            }
                            
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }
                    }
                    
                    // Footer
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Say 'Help' anytime to see this list",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CommandItem(
    command: VoiceCommand,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Command phrase
            Text(
                text = command.phrase,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            // Example usage
            Text(
                text = command.example,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        
        // Description
        Text(
            text = command.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}

private fun defaultVoiceCommands(): List<VoiceCommand> = listOf(
    // Navigation
    VoiceCommand(
        phrase = "Go home",
        example = "'Go to home screen'",
        description = "Navigate to the home screen",
        category = "Navigation"
    ),
    VoiceCommand(
        phrase = "Open settings",
        example = "'Show settings'",
        description = "Open the app settings",
        category = "Navigation"
    ),
    
    // Note Editing
    VoiceCommand(
        phrase = "New note",
        example = "'Create a new note'",
        description = "Start a new note",
        category = "Note Editing"
    ),
    VoiceCommand(
        phrase = "Save note",
        example = "'Save this note'",
        description = "Save the current note",
        category = "Note Editing"
    ),
    VoiceCommand(
        phrase = "Delete note",
        example = "'Delete this note'",
        description = "Delete the current note",
        category = "Note Editing"
    ),
    
    // Formatting
    VoiceCommand(
        phrase = "Bold text",
        example = "'Make this bold'",
        description = "Apply bold formatting to selected text",
        category = "Formatting"
    ),
    VoiceCommand(
        phrase = "Italic text",
        example = "'Italicize this'",
        description = "Apply italic formatting to selected text",
        category = "Formatting"
    ),
    
    // AI Features
    VoiceCommand(
        phrase = "Summarize",
        example = "'Summarize this note'",
        description = "Generate a summary of the current note",
        category = "AI Features"
    ),
    VoiceCommand(
        phrase = "Improve writing",
        example = "'Make this sound better'",
        description = "Enhance the writing in the current note",
        category = "AI Features"
    ),
    
    // Voice Control
    VoiceCommand(
        phrase = "Start listening",
        example = "'Hey AI, listen'",
        description = "Activate voice command mode",
        category = "Voice Control"
    ),
    VoiceCommand(
        phrase = "Stop listening",
        example = "'That's all'",
        description = "Deactivate voice command mode",
        category = "Voice Control"
    ),
    VoiceCommand(
        phrase = "Help",
        example = "'What can I say?'",
        description = "Show available voice commands",
        category = "Voice Control"
    )
)

data class VoiceCommand(
    val phrase: String,
    val example: String,
    val description: String,
    val category: String
)
