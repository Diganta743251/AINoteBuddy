package com.ainotebuddy.app.integration.tasks.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ainotebuddy.app.R
import com.ainotebuddy.app.integration.tasks.model.Task
import com.ainotebuddy.app.integration.tasks.viewmodel.TaskUiState
import com.ainotebuddy.app.integration.tasks.viewmodel.TaskViewModel
import com.ainotebuddy.app.ui.components.*
import java.time.LocalDateTime

/**
 * Main screen for managing tasks.
 */
@Composable
fun TaskScreen(
    noteId: Long,
    onBackClick: () -> Unit,
    viewModel: TaskViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    // Load tasks when the screen is first displayed
    LaunchedEffect(noteId) {
        viewModel.loadTasksForNote(noteId)
    }
    
    val uiState by viewModel.uiState.collectAsState()
    var showAddTaskDialog by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    
    // Show error dialog if there's an error
    if (uiState.error != null) {
        ErrorDialog(
            message = uiState.error!!,
            onDismiss = { viewModel.clearError() }
        )
    }
    
    // Show loading indicator if loading
    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    }
    
    // Main content
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Show completed tasks toggle
                    IconButton(
                        onClick = { viewModel.toggleShowCompleted() }
                    ) {
                        Icon(
                            imageVector = if (uiState.showCompleted) {
                                Icons.Default.VisibilityOff
                            } else {
                                Icons.Default.Visibility
                            },
                            contentDescription = if (uiState.showCompleted) {
                                "Hide completed tasks"
                            } else {
                                "Show completed tasks"
                            }
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    selectedTask = null
                    showAddTaskDialog = true
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add task"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Task list
            TaskList(
                tasks = uiState.tasks,
                showCompleted = uiState.showCompleted,
                onTaskClick = { task ->
                    selectedTask = task
                    showAddTaskDialog = true
                },
                onTaskCheckedChange = { task, isChecked ->
                    viewModel.updateTaskCompletion(task.id, isChecked)
                },
                onTaskDelete = { task ->
                    viewModel.deleteTask(task)
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Add/Edit task dialog
            if (showAddTaskDialog) {
                TaskEditor(
                    task = selectedTask,
                    onDismiss = { showAddTaskDialog = false },
                    onSave = { title, description, dueDate, priority ->
                        if (selectedTask == null) {
                            // Create new task
                            viewModel.createTask(
                                title = title,
                                description = description,
                                dueDate = dueDate,
                                priority = priority
                            )
                        } else {
                            // Update existing task
                            val updatedTask = selectedTask!!.copy(
                                title = title,
                                description = description,
                                dueDate = dueDate,
                                priority = priority
                            )
                            viewModel.updateTask(updatedTask)
                        }
                        showAddTaskDialog = false
                    }
                )
            }
        }
    }
}

/**
 * A simple error dialog.
 */
@Composable
private fun ErrorDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Error") },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}
