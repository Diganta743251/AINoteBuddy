package com.ainotebuddy.app.ui.components.tasks

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import com.ainotebuddy.app.integration.tasks.model.Task
import com.ainotebuddy.app.ui.theme.LocalSpacing

/**
 * Displays a small indicator showing the number of tasks and completed tasks for a note.
 */
@Composable
fun TaskIndicator(
    totalTasks: Int,
    completedTasks: Int,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    val spacing = LocalSpacing.current
    
    Surface(
        onClick = { onClick?.invoke() },
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            // Completed tasks
            Text(
                text = "$completedTasks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
            
            // Separator
            Text(
                text = "/",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Total tasks
            Text(
                text = "$totalTasks",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Task icon
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = if (completedTasks == totalTasks) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Preview for the TaskIndicator composable.
 */
@Preview(showBackground = true)
@Composable
fun TaskIndicatorPreview() {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(16.dp)
    ) {
        // No tasks
        TaskIndicator(
            totalTasks = 0,
            completedTasks = 0
        )
        
        // Some tasks completed
        TaskIndicator(
            totalTasks = 3,
            completedTasks = 1
        )
        
        // All tasks completed
        TaskIndicator(
            totalTasks = 3,
            completedTasks = 3
        )
    }
}
