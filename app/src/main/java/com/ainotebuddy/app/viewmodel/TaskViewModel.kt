package com.ainotebuddy.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.Task
import com.ainotebuddy.app.repository.TaskRepository
import com.ainotebuddy.app.repository.TaskCount
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for the task list screen.
 */
data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalTasks: Int = 0,
    val completedTasks: Int = 0
)

/**
 * ViewModel for managing task-related UI state and interactions.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState(isLoading = true))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private var currentNoteId: Long = -1L

    /**
     * Load tasks for a specific note.
     */
    fun loadTasks(noteId: Long) {
        currentNoteId = noteId
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            try {
                // Load tasks for the note
                taskRepository.getTasksForNote(noteId).collectLatest { tasks ->
                    // Calculate task counts
                    val total = tasks.size
                    val completed = tasks.count { it.isCompleted }
                    
                    _uiState.value = _uiState.value.copy(
                        tasks = tasks,
                        isLoading = false,
                        totalTasks = total,
                        completedTasks = completed
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to load tasks"
                )
            }
        }
    }

    /**
     * Add a new task.
     */
    fun addTask(title: String) {
        if (title.isBlank()) return
        
        viewModelScope.launch {
            try {
                val task = Task(
                    noteId = currentNoteId,
                    title = title,
                    isCompleted = false,
                    position = _uiState.value.tasks.size
                )
                taskRepository.addTask(task)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to add task"
                )
            }
        }
    }

    /**
     * Update an existing task.
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.updateTask(task)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update task"
                )
            }
        }
    }

    /**
     * Delete a task.
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete task"
                )
            }
        }
    }

    /**
     * Toggle the completion status of a task.
     */
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskCompletion(task)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to update task status"
                )
            }
        }
    }

    /**
     * Clear any error message.
     */
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    /**
     * Delete all completed tasks for the current note.
     */
    fun deleteCompletedTasks() {
        viewModelScope.launch {
            try {
                taskRepository.deleteCompletedTasks(currentNoteId)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message ?: "Failed to delete completed tasks"
                )
            }
        }
    }
}
