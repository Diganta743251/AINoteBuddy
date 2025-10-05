package com.ainotebuddy.app.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ainotebuddy.app.ai.AIResponse
import com.ainotebuddy.app.ai.AIResponseType

/**
 * Smart Assist Dialog - Main AI interaction interface
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartAssistDialog(
    isVisible: Boolean,
    noteTitle: String,
    noteContent: String,
    onDismiss: () -> Unit,
    onAIRequest: (String) -> Unit,
    aiResponse: AIResponse?,
    isProcessing: Boolean,
    onApplyResponse: (String) -> Unit
) {
    if (isVisible) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnBackPress = true,
                dismissOnClickOutside = false
            )
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 8.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Header
                    SmartAssistHeader(onDismiss = onDismiss)
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Quick Actions
                    QuickActionChips(
                        onActionSelected = onAIRequest,
                        hasContent = noteContent.isNotBlank()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Custom Request Input
                    CustomRequestInput(
                        onRequest = onAIRequest,
                        isProcessing = isProcessing
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // AI Response Area
                    AIResponseArea(
                        response = aiResponse,
                        isProcessing = isProcessing,
                        onApplyResponse = onApplyResponse,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SmartAssistHeader(onDismiss: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "AI Assistant",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "Smart Assist",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "AI-powered note assistant",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Close"
            )
        }
    }
}

@Composable
private fun QuickActionChips(
    onActionSelected: (String) -> Unit,
    hasContent: Boolean
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (hasContent) {
                items(contentActions) { action ->
                    ActionChip(
                        action = action,
                        onClick = { onActionSelected(action.command) }
                    )
                }
            } else {
                items(generationActions) { action ->
                    ActionChip(
                        action = action,
                        onClick = { onActionSelected(action.command) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActionChip(
    action: QuickAction,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = { Text(action.label) },
        leadingIcon = {
            Icon(
                imageVector = action.icon,
                contentDescription = action.label,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomRequestInput(
    onRequest: (String) -> Unit,
    isProcessing: Boolean
) {
    var customRequest by remember { mutableStateOf("") }
    
    Column {
        Text(
            text = "Custom Request",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = customRequest,
                onValueChange = { customRequest = it },
                placeholder = { Text("Ask me anything about your note...") },
                modifier = Modifier.weight(1f),
                enabled = !isProcessing,
                singleLine = true
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            IconButton(
                onClick = {
                    if (customRequest.isNotBlank()) {
                        onRequest(customRequest)
                        customRequest = ""
                    }
                },
                enabled = customRequest.isNotBlank() && !isProcessing
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send Request"
                )
            }
        }
    }
}

@Composable
private fun AIResponseArea(
    response: AIResponse?,
    isProcessing: Boolean,
    onApplyResponse: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "AI Response",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        Card(
            modifier = Modifier.fillMaxSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                when {
                    isProcessing -> {
                        ProcessingIndicator()
                    }
                    response != null -> {
                        AIResponseContent(
                            response = response,
                            onApplyResponse = onApplyResponse
                        )
                    }
                    else -> {
                        EmptyResponseState()
                    }
                }
            }
        }
    }
}

@Composable
private fun ProcessingIndicator() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "AI is thinking...",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "This may take a few seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AIResponseContent(
    response: AIResponse,
    onApplyResponse: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            // Response Type Badge
            ResponseTypeBadge(type = response.type)
        }
        
        item {
            // Response Content
            SelectionContainer {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Text(
                        text = response.content,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        
        item {
            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onApplyResponse(response.content) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Apply",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Apply")
                }
                
                OutlinedButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(response.content))
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        // Metadata (if available)
        if (response.metadata.isNotEmpty()) {
            item {
                MetadataCard(metadata = response.metadata)
            }
        }
    }
}

@Composable
private fun ResponseTypeBadge(type: AIResponseType) {
    val (color, icon, label) = when (type) {
        AIResponseType.ENHANCEMENT -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.AutoFixHigh,
            "Enhanced"
        )
        AIResponseType.SUMMARY -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.Summarize,
            "Summary"
        )
        AIResponseType.TASKS -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Default.Task,
            "Tasks"
        )
        AIResponseType.GENERATION -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Create,
            "Generated"
        )
        AIResponseType.FLASHCARDS -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.Quiz,
            "Flashcards"
        )
        AIResponseType.EXPANSION -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Default.ExpandMore,
            "Expanded"
        )
        AIResponseType.SHORTENING -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Compress,
            "Shortened"
        )
        AIResponseType.FORMATTING -> Triple(
            MaterialTheme.colorScheme.secondary,
            Icons.Default.FormatAlignLeft,
            "Formatted"
        )
        AIResponseType.VOICE_PROCESSING -> Triple(
            MaterialTheme.colorScheme.tertiary,
            Icons.Default.Mic,
            "Voice Note"
        )
        AIResponseType.GENERAL -> Triple(
            MaterialTheme.colorScheme.primary,
            Icons.Default.Psychology,
            "AI Response"
        )
    }
    
    Surface(
        color = color.copy(alpha = 0.1f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun MetadataCard(metadata: Map<String, String>) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = "Details",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            metadata.forEach { (key, value) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = key.replace("_", " ").replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = value,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyResponseState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Psychology,
            contentDescription = "AI Assistant",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Ready to assist!",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Choose a quick action or type a custom request",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp)
        )
    }
}

// Data classes for quick actions
data class QuickAction(
    val label: String,
    val command: String,
    val icon: ImageVector
)

private val contentActions = listOf(
    QuickAction("Improve", "improve this note", Icons.Default.AutoFixHigh),
    QuickAction("Summarize", "summarize this note", Icons.Default.Summarize),
    QuickAction("Extract Tasks", "extract tasks from this", Icons.Default.Task),
    QuickAction("Create Flashcards", "create flashcards", Icons.Default.Quiz),
    QuickAction("Expand", "expand this content", Icons.Default.ExpandMore),
    QuickAction("Shorten", "shorten this content", Icons.Default.Compress),
    QuickAction("Format", "format this content", Icons.Default.FormatAlignLeft)
)

private val generationActions = listOf(
    QuickAction("Meeting Note", "create meeting note", Icons.Default.Groups),
    QuickAction("Study Note", "create study note", Icons.Default.School),
    QuickAction("Project Note", "create project note", Icons.Default.Assignment),
    QuickAction("Idea Note", "create idea note", Icons.Default.Lightbulb),
    QuickAction("General Note", "create general note", Icons.Default.Note)
)