package com.ainotebuddy.app.repository

import android.content.Context
import com.ainotebuddy.app.data.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Repository interface for task-related data operations.
 */
interface TaskRepository {
    /**
     * Get all tasks for a specific note.
     */
    fun getTasksForNote(noteId: Long): Flow<List<Task>>

    /**
     * Get task counts (total and completed) for all notes.
     */
    fun getTaskCounts(): Flow<Map<Long, TaskCount>>

    /**
     * Get task counts for a specific note.
     */
    suspend fun getTaskCountForNote(noteId: Long): TaskCount

    /**
     * Add a new task.
     */
    suspend fun addTask(task: Task)

    /**
     * Update an existing task.
     */
    suspend fun updateTask(task: Task)

    /**
     * Delete a task.
     */
    suspend fun deleteTask(task: Task)
    
    /**
     * Delete all tasks for a specific note.
     */
    suspend fun deleteTasksForNote(noteId: Long)
    
    /**
     * Delete all completed tasks for a specific note.
     */
    suspend fun deleteCompletedTasks(noteId: Long)
    
    /**
     * Delete all tasks from the database.
     */
    suspend fun deleteAllTasks()
    
    /**
     * Toggle the completion status of a task.
     */
    suspend fun toggleTaskCompletion(task: Task)
}

/**
 * Repository for managing task data operations.
 */
class TaskRepositoryImpl(private val context: Context) : TaskRepository {
    // In-memory storage for demo purposes
    private val tasks = mutableMapOf<Long, MutableList<Task>>()
    private var nextId = 1L
    
    /**
     * Get all tasks for a specific note.
     */
    override fun getTasksForNote(noteId: Long): Flow<List<Task>> {
        return flow {
            emit(tasks[noteId]?.toList() ?: emptyList())
        }
    }

    /**
     * Get task counts (total and completed) for all notes.
     */
    override fun getTaskCounts(): Flow<Map<Long, TaskCount>> {
        return flow {
            val counts = mutableMapOf<Long, TaskCount>()
            tasks.forEach { (noteId, taskList) ->
                val total = taskList.size
                val completed = taskList.count { it.isCompleted }
                counts[noteId] = TaskCount(total, completed)
            }
            emit(counts)
        }
    }

    /**
     * Get task counts for a specific note.
     */
    override suspend fun getTaskCountForNote(noteId: Long): TaskCount {
        val taskList = tasks[noteId] ?: return TaskCount(0, 0)
        val total = taskList.size
        val completed = taskList.count { it.isCompleted }
        return TaskCount(total, completed)
    }

    /**
     * Add a new task.
     */
    override suspend fun addTask(task: Task) {
        val newId = nextId++
        val newTask = task.copy(id = newId)
        
        if (!tasks.containsKey(task.noteId)) {
            tasks[task.noteId] = mutableListOf()
        }
        
        tasks[task.noteId]?.add(newTask)
    }

    /**
     * Update an existing task.
     */
    override suspend fun updateTask(task: Task) {
        tasks[task.noteId]?.let { taskList ->
            val index = taskList.indexOfFirst { it.id == task.id }
            if (index != -1) {
                taskList[index] = task
            }
        }
    }

    /**
     * Delete a task.
     */
    override suspend fun deleteTask(task: Task) {
        tasks[task.noteId]?.removeIf { it.id == task.id }
    }

    /**
     * Delete completed tasks for a specific note.
     */
    override suspend fun deleteCompletedTasks(noteId: Long) {
        tasks[noteId]?.removeIf { it.isCompleted }
    }
    
    /**
     * Delete all tasks for a specific note.
     */
    override suspend fun deleteTasksForNote(noteId: Long) {
        tasks.remove(noteId)
    }
    
    /**
     * Delete all tasks from the database.
     */
    override suspend fun deleteAllTasks() {
        tasks.clear()
    }

    /**
     * Toggle the completion status of a task.
     */
    override suspend fun toggleTaskCompletion(task: Task) {
        tasks[task.noteId]?.let { taskList ->
            val index = taskList.indexOfFirst { it.id == task.id }
            if (index != -1) {
                taskList[index] = taskList[index].copy(isCompleted = !taskList[index].isCompleted)
            }
        }
    }
}

/**
 * Data class representing the count of total and completed tasks.
 */
data class TaskCount(
    val total: Int,
    val completed: Int
)
