package com.ainotebuddy.app.data.repository

import com.ainotebuddy.app.data.local.dao.TaskDao
import com.ainotebuddy.app.data.model.Task
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    fun getTasksByNoteId(noteId: Long): Flow<List<Task>> {
        return taskDao.getTasksByNoteId(noteId)
    }

    fun getTaskStats(noteId: Long): Flow<Pair<Int, Int>> {
        return taskDao.getTotalTaskCount(noteId).combine(
            taskDao.getCompletedTaskCount(noteId)
        ) { total, completed ->
            total to completed
        }
    }

    suspend fun addTask(task: Task): Long {
        // Increment positions for existing tasks
        taskDao.incrementPositions(task.noteId, 0)
        return taskDao.insertTask(task.copy(position = 0))
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
        // Decrement positions for tasks that were after the deleted one
        taskDao.decrementPositions(task.noteId, task.position)
    }

    suspend fun toggleTaskCompletion(task: Task) {
        taskDao.updateTask(task.copy(isCompleted = !task.isCompleted))
    }

    suspend fun moveTask(taskId: Long, fromPosition: Int, toPosition: Int) {
        // This is a simplified implementation
        // In a real app, you'd need to handle the position updates more carefully
        taskDao.updateTask(taskDao.getTaskById(taskId).copy(position = toPosition))
    }

    suspend fun deleteTasksForNote(noteId: Long) {
        taskDao.deleteTasksForNote(noteId)
    }
}

// Extension function to combine two Flows
private fun <T1, T2, R> Flow<T1>.combine(
    flow: Flow<T2>,
    transform: suspend (T1, T2) -> R
): Flow<R> {
    return kotlinx.coroutines.flow.combine(this, flow, transform)
}

// Extension function to get task by ID (simplified for this example)
private suspend fun TaskDao.getTaskById(id: Long): Task {
    // In a real app, you'd implement this properly in the DAO
    return getTasksByNoteId(0).first().first { it.id == id }
}
