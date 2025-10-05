package com.ainotebuddy.app.ui.components.ai

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.Devices
import com.ainotebuddy.app.ai.AISuggestion
import com.ainotebuddy.app.ai.SuggestionType
import com.ainotebuddy.app.ui.theme.LocalSpacing

/**
 * A horizontal scrollable bar that displays AI suggestions
 */
@Composable
fun AISuggestionsBar(
    suggestions: List<AISuggestion>,
    onSuggestionClick: (AISuggestion) -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    onDismiss: (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    var isExpanded by remember { mutableStateOf(true) }
    val rotation by animateFloatAsState(if (isExpanded) 180f else 0f)
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(vertical = 4.dp)
    ) {
        // Header with title and controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = spacing.medium, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            
            Spacer(modifier = Modifier.width(spacing.small))
            
            Text(
                text = "AI Suggestions",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Toggle expand/collapse
            IconButton(
                onClick = { isExpanded = !isExpanded },
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ExpandLess,
                    contentDescription = if (isExpanded) "Collapse" else "Expand",
                    modifier = Modifier.rotate(rotation)
                )
            }
            
            // Dismiss button if provided
            onDismiss?.let { onDismiss ->
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss"
                    )
                }
            }
        }
        
        // Suggestions list
        AnimatedVisibility(visible = isExpanded) {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
            } else if (suggestions.isEmpty()) {
                Text(
                    text = "No suggestions available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = spacing.medium, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(spacing.small)
                ) {
                    items(suggestions, key = { it.id }) { suggestion ->
                        SuggestionChip(
                            suggestion = suggestion,
                            onClick = { onSuggestionClick(suggestion) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single suggestion chip
 */
@Composable
private fun SuggestionChip(
    suggestion: AISuggestion,
    onClick: () -> Unit
) {
    val icon = when (suggestion.type) {
        SuggestionType.TAGGING -> Icons.Default.Label
        SuggestionType.CONTENT_IMPROVEMENT -> Icons.Default.AutoFixHigh
        SuggestionType.RELATED_NOTE -> Icons.Default.Link
        SuggestionType.PRODUCTIVITY -> Icons.Default.AutoAwesome
        SuggestionType.FORMATTING -> Icons.Default.FormatColorText
        SuggestionType.ORGANIZATION -> Icons.Default.Folder
    }
    
    val containerColor = when (suggestion.type) {
        SuggestionType.TAGGING -> MaterialTheme.colorScheme.primaryContainer
        SuggestionType.CONTENT_IMPROVEMENT -> MaterialTheme.colorScheme.secondaryContainer
        SuggestionType.RELATED_NOTE -> MaterialTheme.colorScheme.tertiaryContainer
        SuggestionType.PRODUCTIVITY -> MaterialTheme.colorScheme.secondaryContainer
        SuggestionType.FORMATTING -> MaterialTheme.colorScheme.surfaceVariant
        SuggestionType.ORGANIZATION -> MaterialTheme.colorScheme.surfaceVariant
    }
    
    val contentColor = when (suggestion.type) {
        SuggestionType.TAGGING -> MaterialTheme.colorScheme.onPrimaryContainer
        SuggestionType.CONTENT_IMPROVEMENT -> MaterialTheme.colorScheme.onSecondaryContainer
        SuggestionType.RELATED_NOTE -> MaterialTheme.colorScheme.onTertiaryContainer
        SuggestionType.PRODUCTIVITY -> MaterialTheme.colorScheme.onSecondaryContainer
        SuggestionType.FORMATTING -> MaterialTheme.colorScheme.onSurfaceVariant
        SuggestionType.ORGANIZATION -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = suggestion.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.labelMedium
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = containerColor,
            labelColor = contentColor,
            leadingIconContentColor = contentColor
        ),
        border = null,
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.animateContentSize()
    )
}

@Preview(device = Devices.PIXEL_4)
@Composable
private fun AISuggestionsBarPreview() {
    val sampleSuggestions = listOf(
        AISuggestion("1", SuggestionType.TAGGING, "meeting", "Suggested tag: meeting", 0.9f),
        AISuggestion("2", SuggestionType.CONTENT_IMPROVEMENT, "Continue writing about...", "Content extension", 0.85f),
        AISuggestion("3", SuggestionType.RELATED_NOTE, "See related notes", "Open related notes", 0.8f),
        AISuggestion("4", SuggestionType.PRODUCTIVITY, "Set reminder", "Create a reminder", 0.75f),
        AISuggestion("5", SuggestionType.FORMATTING, "Format as list", "Apply list formatting", 0.7f)
    )
    
    MaterialTheme {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                AISuggestionsBar(
                    suggestions = sampleSuggestions,
                    onSuggestionClick = {},
                    isLoading = false,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                AISuggestionsBar(
                    suggestions = emptyList(),
                    onSuggestionClick = {},
                    isLoading = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
