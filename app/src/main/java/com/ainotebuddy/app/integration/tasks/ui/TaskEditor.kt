package com.ainotebuddy.app.integration.tasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ainotebuddy.app.R
import com.ainotebuddy.app.integration.tasks.model.Task
import com.ainotebuddy.app.ui.components.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * A dialog for adding or editing a task.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditor(
    task: Task? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        dueDate: LocalDateTime?,
        priority: Int
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var dueDate by remember { mutableStateOf<LocalDateTime?>(task?.dueDate) }
    var priority by remember { mutableStateOf(task?.priority ?: 3) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (task == null) "Add Task" else "Edit Task",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Task title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                // Task description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    singleLine = false
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Priority selector
                Text("Priority", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                PrioritySelector(
                    selectedPriority = priority,
                    onPrioritySelected = { priority = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Due date
                Text("Due Date", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                DateTimePickerRow(
                    dateTime = dueDate ?: LocalDateTime.now(),
                    showTime = true,
                    onDateTimeSelected = { dateTime ->
                        dueDate = dateTime
                    },
                    onClear = { dueDate = null },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("CANCEL")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            onSave(
                                title.trim(),
                                description.trim(),
                                dueDate,
                                priority
                            )
                        },
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (task == null) "ADD" else "SAVE")
                    }
                }
            }
        }
    }
}

/**
 * A row of priority buttons for selecting task priority.
 */
@Composable
private fun PrioritySelector(
    selectedPriority: Int,
    onPrioritySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fillMaxWidth()
    ) {
        // Priority 1 (Lowest)
        PriorityButton(
            priority = 1,
            label = "Lowest",
            isSelected = selectedPriority == 1,
            onClick = { onPrioritySelected(1) }
        )
        
        // Priority 2 (Low)
        PriorityButton(
            priority = 2,
            label = "Low",
            isSelected = selectedPriority == 2,
            onClick = { onPrioritySelected(2) }
        )
        
        // Priority 3 (Medium)
        PriorityButton(
            priority = 3,
            label = "Medium",
            isSelected = selectedPriority == 3,
            onClick = { onPrioritySelected(3) }
        )
        
        // Priority 4 (High)
        PriorityButton(
            priority = 4,
            label = "High",
            isSelected = selectedPriority == 4,
            onClick = { onPrioritySelected(4) }
        )
        
        // Priority 5 (Highest)
        PriorityButton(
            priority = 5,
            label = "Highest",
            isSelected = selectedPriority == 5,
            onClick = { onPrioritySelected(5) }
        )
    }
}

/**
 * A single priority selection button.
 */
@Composable
private fun PriorityButton(
    priority: Int,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = androidx.compose.material3.ButtonDefaults.buttonColors(
        containerColor = if (isSelected) getPriorityColor(priority) 
                         else MaterialTheme.colorScheme.surfaceVariant,
        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                      else MaterialTheme.colorScheme.onSurfaceVariant
    )
    
    Button(
        onClick = onClick,
        colors = colors,
        shape = MaterialTheme.shapes.small,
        modifier = modifier.size(56.dp, 32.dp),
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = priority.toString(),
            style = MaterialTheme.typography.labelMedium
        )
    }
}

/**
 * A row for selecting date and time with a clear button.
 */
@Composable
private fun DateTimePickerRow(
    dateTime: LocalDateTime?,
    showTime: Boolean = true,
    onDateTimeSelected: (LocalDateTime) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        // Date button
        OutlinedButton(
            onClick = { /* Show date picker */ },
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = dateTime?.format(dateFormatter) ?: "Select date",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        
        if (showTime) {
            Spacer(modifier = Modifier.width(8.dp))
            
            // Time button
            OutlinedButton(
                onClick = { /* Show time picker */ },
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = dateTime?.format(timeFormatter) ?: "Select time",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        Spacer(modifier = Modifier.width(8.dp))
        
        // Clear button
        IconButton(
            onClick = onClear,
            enabled = dateTime != null
        ) {
            Icon(
                imageVector = Icons.Default.Clear,
                contentDescription = "Clear date/time"
            )
        }
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
