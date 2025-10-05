package com.ainotebuddy.app.integration.tasks.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.integration.tasks.model.Task
import com.ainotebuddy.app.integration.tasks.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * UI state for the task management screen.
 */
data class TaskUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val tasks: List<Task> = emptyList(),
    val selectedTask: Task? = null,
    val showCompleted: Boolean = false
)

/**
 * ViewModel for managing tasks in the application.
 */
@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private var currentNoteId: Long? = null

    /**
     * Loads tasks for a specific note.
     * @param noteId The ID of the note to load tasks for
     */
    fun loadTasksForNote(noteId: Long) {
        currentNoteId = noteId
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                taskRepository.getTasksForNote(noteId)
                    .collect { tasks ->
                        _uiState.update { state ->
                            state.copy(
                                tasks = tasks,
                                isLoading = false
                            )
                        }
                    }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to load tasks"
                    )
                }
            }
        }
    }

    /**
     * Creates a new task from the current note.
     * @param title The title of the task
     * @param description The description of the task
     * @param dueDate The due date of the task (optional)
     * @param priority The priority of the task (1-5, with 5 being highest)
     */
    fun createTask(
        title: String,
        description: String = "",
        dueDate: LocalDateTime? = null,
        priority: Int = 3
    ) {
        val noteId = currentNoteId ?: return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                taskRepository.createTask(
                    noteId = noteId,
                    title = title,
                    description = description,
                    dueDate = dueDate,
                    priority = priority
                )
                // The Flow will automatically update the UI
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to create task"
                    )
                }
            }
        }
    }

    /**
     * Updates the completion status of a task.
     * @param taskId The ID of the task to update
     * @param isCompleted Whether the task is completed
     */
    fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                taskRepository.updateTaskCompletion(taskId, isCompleted)
                // The Flow will automatically update the UI
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update task"
                    )
                }
            }
        }
    }

    /**
     * Updates the priority of a task.
     * @param taskId The ID of the task to update
     * @param priority The new priority (1-5, with 5 being highest)
     */
    fun updateTaskPriority(taskId: Long, priority: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                taskRepository.updateTaskPriority(taskId, priority)
                // The Flow will automatically update the UI
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update task priority"
                    )
                }
            }
        }
    }

    /**
     * Updates the due date of a task.
     * @param taskId The ID of the task to update
     * @param dueDate The new due date, or null to remove the due date
     */
    fun updateTaskDueDate(taskId: Long, dueDate: LocalDateTime?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            
            try {
                taskRepository.updateTaskDueDate(taskId, dueDate)
                // The Flow will automatically update the UI
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update due date"
                    )
                }
            }
        }
    }
    /**
     * Deletes a task.
     * @param task The task to delete
     */
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                taskRepository.deleteTask(task)
                // The Flow will automatically update the UI
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to delete task"
                    )
                }
            }
        }
    }

    /**
     * Updates an existing task with new values.
     * @param task The updated task instance
     */
    fun updateTask(task: Task) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                taskRepository.updateTask(task)
                // Flow will emit updated list
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to update task"
                    )
                }
            }
        }
    }

    /**
     * Selects a task for editing.
     * @param task The task to select, or null to clear the selection
     */
    fun selectTask(task: Task?) {
        _uiState.update { it.copy(selectedTask = task) }
    }

    /**
     * Toggles the visibility of completed tasks.
     */
    fun toggleShowCompleted() {
        _uiState.update { it.copy(showCompleted = !it.showCompleted) }
    }

    /**
     * Clears any error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
