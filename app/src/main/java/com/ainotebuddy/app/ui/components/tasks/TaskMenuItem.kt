package com.ainotebuddy.app.ui.components.tasks

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.R

/**
 * A menu item for the note editor to create or manage tasks.
 */
@Composable
fun TaskMenuItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hasTasks: Boolean = false,
    allTasksCompleted: Boolean = false
) {
    DropdownMenuItem(
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Task icon with status indicator
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(24.dp)
                ) {
                    if (hasTasks) {
                        if (allTasksCompleted) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Pending,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.AddTask,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Task status indicator
                    if (hasTasks && !allTasksCompleted) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(8.dp)
                                .background(
                                    color = MaterialTheme.colorScheme.primary,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Menu text
                Text(
                    text = if (hasTasks) "Manage Tasks" else "Create Task",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                // Task count badge if there are tasks
                if (hasTasks) {
                    Spacer(modifier = Modifier.weight(1f))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "1", // This would be the actual task count
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        onClick = onClick,
        modifier = modifier
    )
}

/**
 * Preview for the TaskMenuItem composable.
 */
@Preview(showBackground = true)
@Composable
fun TaskMenuItemPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .width(280.dp)
            .padding(16.dp)
    ) {
        // No tasks
        TaskMenuItem(
            onClick = {},
            hasTasks = false
        )
        
        // With incomplete tasks
        TaskMenuItem(
            onClick = {},
            hasTasks = true,
            allTasksCompleted = false
        )
        
        // All tasks completed
        TaskMenuItem(
            onClick = {},
            hasTasks = true,
            allTasksCompleted = true
        )
    }
}
