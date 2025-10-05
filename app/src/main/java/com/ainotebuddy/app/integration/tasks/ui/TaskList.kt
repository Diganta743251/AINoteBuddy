package com.ainotebuddy.app.integration.tasks.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.integration.tasks.model.Task
import com.ainotebuddy.app.ui.components.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Displays a list of tasks with the ability to mark them as complete, edit, and delete.
 */
@Composable
fun TaskList(
    tasks: List<Task>,
    showCompleted: Boolean = false,
    onTaskClick: (Task) -> Unit = {},
    onTaskCheckedChange: (Task, Boolean) -> Unit = { _, _ -> },
    onTaskDelete: (Task) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val filteredTasks = if (showCompleted) {
        tasks
    } else {
        tasks.filter { !it.isCompleted }
    }

    if (filteredTasks.isEmpty()) {
        EmptyTaskList()
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(
                items = filteredTasks,
                key = { it.id }
            ) { task ->
                TaskItem(
                    task = task,
                    onClick = { onTaskClick(task) },
                    onCheckedChange = { isChecked -> onTaskCheckedChange(task, isChecked) },
                    onDelete = { onTaskDelete(task) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Displays a single task item with a checkbox, title, due date, and priority indicator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskItem(
    task: Task,
    onClick: () -> Unit = {},
    onCheckedChange: ((Boolean) -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    Card(
        onClick = onClick,
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
        ) {
            // Priority indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(MaterialTheme.shapes.small)
                    .background(getPriorityColor(task.priority))
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Checkbox and task details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Task title with strikethrough if completed
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                    color = if (task.isCompleted) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
                
                // Due date and time
                if (task.dueDate != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            tint = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else if (isTaskOverdue(task)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDueDate(task.dueDate, task.isCompleted),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (task.isCompleted) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else if (isTaskOverdue(task)) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }
            
            // Checkbox
            if (onCheckedChange != null) {
                Checkbox(
                    checked = task.isCompleted,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = MaterialTheme.colorScheme.primary,
                        checkmarkColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
            
            // Delete button (only show on hover/long-press if needed)
            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

/**
 * Displays an empty state for the task list.
 */
@Composable
private fun EmptyTaskList() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No tasks yet",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = "Tap the + button to add a new task",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

/**
 * Gets the color for a task priority.
 */
@Composable
private fun getPriorityColor(priority: Int): Color {
    return when (priority) {
        5 -> MaterialTheme.colorScheme.error // High priority (urgent)
        4 -> MaterialTheme.colorScheme.errorContainer // High priority
        3 -> MaterialTheme.colorScheme.primaryContainer // Medium priority
        2 -> MaterialTheme.colorScheme.secondaryContainer // Low priority
        1 -> MaterialTheme.colorScheme.tertiaryContainer // Very low priority
        else -> MaterialTheme.colorScheme.surfaceVariant // Default
    }
}

/**
 * Checks if a task is overdue.
 */
private fun isTaskOverdue(task: Task): Boolean {
    return !task.isCompleted && 
           task.dueDate != null && 
           task.dueDate.isBefore(LocalDateTime.now())
}

/**
 * Formats a due date for display.
 */
private fun formatDueDate(dueDate: LocalDateTime?, isCompleted: Boolean): String {
    if (dueDate == null) return ""
    
    val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    
    val now = LocalDateTime.now()
    val today = now.toLocalDate()
    val tomorrow = today.plusDays(1)
    val yesterday = today.minusDays(1)
    
    return when (dueDate.toLocalDate()) {
        today -> "Today, ${dueDate.format(timeFormatter)}"
        tomorrow -> "Tomorrow, ${dueDate.format(timeFormatter)}"
        yesterday -> "Yesterday, ${dueDate.format(timeFormatter)}"
        else -> "${dueDate.format(dateFormatter)}, ${dueDate.format(timeFormatter)}"
    }
}
