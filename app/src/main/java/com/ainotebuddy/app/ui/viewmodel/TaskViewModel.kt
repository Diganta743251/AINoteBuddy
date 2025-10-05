package com.ainotebuddy.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ainotebuddy.app.data.model.Task
import com.ainotebuddy.app.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TaskUiState(
    val tasks: List<Task> = emptyList(),
    val totalTasks: Int = 0,
    val completedTasks: Int = 0,
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class TaskViewModel @Inject constructor(
    private val taskRepository: TaskRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TaskUiState(isLoading = true))
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()
    
    private var currentNoteId: Long = -1L
    
    fun loadTasks(noteId: Long) {
        currentNoteId = noteId
        viewModelScope.launch {
            try {
                taskRepository.getTasksByNoteId(noteId)
                    .combine(taskRepository.getTaskStats(noteId)) { tasks, (total, completed) ->
                        _uiState.update { 
                            it.copy(
                                tasks = tasks.sortedBy { task -> task.position },
                                totalTasks = total,
                                completedTasks = completed,
                                isLoading = false,
                                error = null
                            )
                        }
                    }
                    .collect()
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
    
    fun addTask(title: String) {
        if (title.isBlank()) return
        
        viewModelScope.launch {
            try {
                val task = Task(
                    noteId = currentNoteId,
                    title = title.trim(),
                    position = _uiState.value.tasks.size
                )
                taskRepository.addTask(task)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to add task") 
                }
            }
        }
    }
    
    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.toggleTaskCompletion(task)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to update task") 
                }
            }
        }
    }
    
    fun deleteTask(task: Task) {
        viewModelScope.launch {
            try {
                taskRepository.deleteTask(task)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to delete task") 
                }
            }
        }
    }
    
    fun moveTask(from: Int, to: Int) {
        val tasks = _uiState.value.tasks.toMutableList()
        if (from < 0 || from >= tasks.size || to < 0 || to >= tasks.size) {
            return
        }
        
        viewModelScope.launch {
            try {
                val task = tasks[from]
                taskRepository.moveTask(task.id, from, to)
            } catch (e: Exception) {
                _uiState.update { 
                    it.copy(error = e.message ?: "Failed to move task") 
                }
            }
        }
    }
    
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
