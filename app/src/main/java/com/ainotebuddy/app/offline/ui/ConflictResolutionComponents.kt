package com.ainotebuddy.app.offline.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.ainotebuddy.app.offline.*
import com.ainotebuddy.app.data.NoteEntity
import kotlinx.coroutines.flow.StateFlow

/**
 * UI components for conflict resolution in Enhanced Offline-First Architecture
 * Provides intuitive interfaces for resolving sync conflicts and data inconsistencies
 */

@Composable
fun ConflictResolutionDialog(
    conflict: ConflictData,
    localNote: NoteEntity,
    remoteNote: NoteEntity,
    onResolve: (ConflictResolutionStrategy, NoteEntity?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                ConflictResolutionHeader(
                    conflictType = conflict.conflictType,
                    confidence = conflict.confidence
                )
                
                // Content comparison
                ConflictComparisonView(
                    localNote = localNote,
                    remoteNote = remoteNote,
                    conflictMarkers = conflict.conflictMarkers,
                    modifier = Modifier.weight(1f)
                )
                
                // Resolution options
                ConflictResolutionActions(
                    suggestedStrategy = conflict.suggestedResolution,
                    confidence = conflict.confidence,
                    onResolve = onResolve,
                    onDismiss = onDismiss,
                    localNote = localNote,
                    remoteNote = remoteNote
                )
            }
        }
    }
}

@Composable
fun ConflictResolutionHeader(
    conflictType: ConflictType,
    confidence: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resolve Conflict",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            
            ConflictTypeChip(conflictType = conflictType)
        }
        
        if (confidence > 0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI Confidence",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "AI Confidence: ${(confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                LinearProgressIndicator(
                    progress = confidence,
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = getConfidenceColor(confidence)
                )
            }
        }
    }
}

@Composable
fun ConflictComparisonView(
    localNote: NoteEntity,
    remoteNote: NoteEntity,
    conflictMarkers: List<ConflictMarker>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Compare Versions",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Local version
            ConflictVersionCard(
                title = "Your Version",
                note = localNote,
                isLocal = true,
                modifier = Modifier.weight(1f)
            )
            
            // Remote version
            ConflictVersionCard(
                title = "Server Version",
                note = remoteNote,
                isLocal = false,
                modifier = Modifier.weight(1f)
            )
        }
        
        // Conflict markers if available
        if (conflictMarkers.isNotEmpty()) {
            ConflictMarkersView(
                conflictMarkers = conflictMarkers,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ConflictVersionCard(
    title: String,
    note: NoteEntity,
    isLocal: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isLocal) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else 
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Icon(
                    imageVector = if (isLocal) Icons.Default.PhoneAndroid else Icons.Default.Cloud,
                    contentDescription = title,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Note details
            Column(
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Title: ${note.title}",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Modified: ${formatTimestamp(note.updatedAt)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Version: ${note.version}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Content preview
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Text(
                    text = note.content.take(200) + if (note.content.length > 200) "..." else "",
                    modifier = Modifier
                        .padding(8.dp)
                        .verticalScroll(rememberScrollState()),
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun ConflictMarkersView(
    conflictMarkers: List<ConflictMarker>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Specific Conflicts (${conflictMarkers.size})",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            
            LazyColumn(
                modifier = Modifier.height(150.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(conflictMarkers) { marker ->
                    ConflictMarkerItem(marker = marker)
                }
            }
        }
    }
}

@Composable
fun ConflictMarkerItem(
    marker: ConflictMarker,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Conflict at position ${marker.startIndex}-${marker.endIndex}",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Local:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = marker.localContent,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Remote:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = marker.remoteContent,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
            
            if (marker.suggestion != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(4.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(6.dp)
                    ) {
                        Text(
                            text = "AI Suggestion:",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = marker.suggestion,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ConflictResolutionActions(
    suggestedStrategy: ConflictResolutionStrategy?,
    confidence: Float,
    onResolve: (ConflictResolutionStrategy, NoteEntity?) -> Unit,
    onDismiss: () -> Unit,
    localNote: NoteEntity,
    remoteNote: NoteEntity,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Choose Resolution Strategy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
        
        // Quick action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Accept local
            OutlinedButton(
                onClick = { onResolve(ConflictResolutionStrategy.ACCEPT_LOCAL, localNote) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.PhoneAndroid,
                    contentDescription = "Accept Local",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Keep Mine")
            }
            
            // Accept remote
            OutlinedButton(
                onClick = { onResolve(ConflictResolutionStrategy.ACCEPT_REMOTE, remoteNote) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.Cloud,
                    contentDescription = "Accept Remote",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Use Server")
            }
        }
        
        // Advanced options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Auto merge (if high confidence)
            if (confidence > 0.7f) {
                Button(
                    onClick = { onResolve(ConflictResolutionStrategy.AUTO_MERGE, null) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoFixHigh,
                        contentDescription = "Auto Merge",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Auto Merge")
                }
            }
            
            // Manual merge
            OutlinedButton(
                onClick = { onResolve(ConflictResolutionStrategy.SIDE_BY_SIDE, null) },
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.CompareArrows,
                    contentDescription = "Manual Merge",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Manual")
            }
        }
        
        // Suggested strategy highlight
        if (suggestedStrategy != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = "Suggestion",
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "AI suggests: ${getStrategyDisplayName(suggestedStrategy)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        // Action buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    }
}

@Composable
fun ConflictTypeChip(
    conflictType: ConflictType,
    modifier: Modifier = Modifier
) {
    val (color, text) = when (conflictType) {
        ConflictType.CONTENT -> Color.Red to "Content"
        ConflictType.METADATA -> Color(0xFFFF9800) to "Metadata" // Orange
        ConflictType.STRUCTURAL -> Color(0xFF9C27B0) to "Structure" // Purple
        ConflictType.COLLABORATIVE -> Color.Blue to "Collaborative"
        ConflictType.VERSION -> Color.Green to "Version"
    }
    
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.1f),
        contentColor = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium
        )
    }
}

// Helper functions
private fun getConfidenceColor(confidence: Float): Color = when {
    confidence > 0.8f -> Color.Green
    confidence > 0.6f -> Color(0xFFFF9800) // Orange
    else -> Color.Red
}

private fun getStrategyDisplayName(strategy: ConflictResolutionStrategy): String = when (strategy) {
    ConflictResolutionStrategy.AUTO_MERGE -> "Automatic Merge"
    ConflictResolutionStrategy.ACCEPT_LOCAL -> "Keep Your Version"
    ConflictResolutionStrategy.ACCEPT_REMOTE -> "Use Server Version"
    ConflictResolutionStrategy.USER_CHOICE -> "Manual Selection"
    ConflictResolutionStrategy.AI_ASSISTED -> "AI-Assisted Merge"
    ConflictResolutionStrategy.THREE_WAY_MERGE -> "Three-Way Merge"
    ConflictResolutionStrategy.SIDE_BY_SIDE -> "Side-by-Side Comparison"
}

private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    
    return when {
        diff < 60000 -> "Just now"
        diff < 3600000 -> "${diff / 60000}m ago"
        diff < 86400000 -> "${diff / 3600000}h ago"
        else -> "${diff / 86400000}d ago"
    }
}
