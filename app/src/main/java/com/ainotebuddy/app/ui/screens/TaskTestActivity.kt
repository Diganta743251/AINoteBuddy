package com.ainotebuddy.app.ui.screens

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ainotebuddy.app.ui.components.tasks.TaskListScreen
import com.ainotebuddy.app.ui.theme.AINoteBuddyTheme
import com.ainotebuddy.app.ui.viewmodel.TaskViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskTestActivity : ComponentActivity() {
    @Inject
    lateinit var taskViewModel: TaskViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            AINoteBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    TaskTestScreen(
                        taskViewModel = taskViewModel,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@Composable
fun TaskTestScreen(
    taskViewModel: TaskViewModel,
    onBack: () -> Unit
) {
    // Test note ID - in a real app, this would be passed from the note editor
    val testNoteId = 1L
    
    // Load tasks for the test note
    LaunchedEffect(Unit) {
        taskViewModel.loadTasks(testNoteId)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Back button
        Button(onClick = onBack) {
            Text("Back to Main App")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Task list screen
        TaskListScreen(
            viewModel = taskViewModel,
            onBack = onBack,
            modifier = Modifier.weight(1f)
        )
    }
}
