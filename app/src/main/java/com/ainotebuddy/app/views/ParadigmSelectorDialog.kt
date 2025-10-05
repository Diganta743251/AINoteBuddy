package com.ainotebuddy.app.views

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

/**
 * Dialog for selecting organization paradigm
 */
@Composable
fun ParadigmSelectorDialog(
    currentParadigm: OrganizationParadigm,
    onDismiss: () -> Unit,
    onParadigmSelected: (OrganizationParadigm) -> Unit
) {
    val paradigms = remember {
        listOf(
            ParadigmOption(
                paradigm = OrganizationParadigm.INTELLIGENT_AUTO,
                title = "AI Auto",
                description = "Let AI choose the best organization method",
                icon = Icons.Default.AutoAwesome,
                recommended = true
            ),
            ParadigmOption(
                paradigm = OrganizationParadigm.TIME_BASED,
                title = "Time-Based",
                description = "Organize by creation date and time patterns",
                icon = Icons.Default.Schedule
            ),
            ParadigmOption(
                paradigm = OrganizationParadigm.TOPIC_BASED,
                title = "Topic-Based",
                description = "Group by AI-detected topics and themes",
                icon = Icons.Default.Topic
            ),
            ParadigmOption(
                paradigm = OrganizationParadigm.PRIORITY_BASED,
                title = "Priority-Based",
                description = "Sort by urgency and importance levels",
                icon = Icons.Default.PriorityHigh
            ),
            ParadigmOption(
                paradigm = OrganizationParadigm.PROJECT_BASED,
                title = "Project-Based",
                description = "Organize by identified projects and workflows",
                icon = Icons.Default.Work
            ),
            ParadigmOption(
                paradigm = OrganizationParadigm.SENTIMENT_BASED,
                title = "Sentiment-Based",
                description = "Group by emotional tone and sentiment",
                icon = Icons.Default.SentimentSatisfied
            )
        )
    }
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Organization Method",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                Text(
                    text = "Choose how you want your notes organized",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
                
                // Paradigm options
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 400.dp)
                ) {
                    items(paradigms) { option ->
                        ParadigmOptionCard(
                            option = option,
                            isSelected = option.paradigm == currentParadigm,
                            onSelect = { onParadigmSelected(option.paradigm) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
}

@Composable
private fun ParadigmOptionCard(
    option: ParadigmOption,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = isSelected,
                onClick = onSelect,
                role = Role.RadioButton
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            }
        ),
        border = if (isSelected) {
            CardDefaults.outlinedCardBorder().copy(
                width = 2.dp,
                brush = androidx.compose.ui.graphics.SolidColor(MaterialTheme.colorScheme.primary)
            )
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Icon(
                imageVector = option.icon,
                contentDescription = null,
                tint = if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                    
                    if (option.recommended) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.tertiary
                        ) {
                            Text(
                                text = "Recommended",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Text(
                    text = option.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            
            // Selection indicator
            RadioButton(
                selected = isSelected,
                onClick = null // Handled by card click
            )
        }
    }
}

/**
 * Data class representing a paradigm option
 */
private data class ParadigmOption(
    val paradigm: OrganizationParadigm,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val recommended: Boolean = false
)
