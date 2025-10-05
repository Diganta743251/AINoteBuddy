package com.ainotebuddy.app.integration.tasks.repository

import com.ainotebuddy.app.integration.tasks.data.TaskDao
import com.ainotebuddy.app.integration.tasks.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime
import javax.inject.Inject

/**
 * Repository for managing tasks in the application.
 */
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao
) {
    /**
     * Gets all tasks from the database.
     * @return A Flow emitting the list of all tasks
     */
    fun getAllTasks(): Flow<List<Task>> = taskDao.getAllTasks()

    /**
     * Gets tasks for a specific note.
     * @param noteId The ID of the note
     * @return A Flow emitting the list of tasks for the note
     */
    fun getTasksForNote(noteId: Long): Flow<List<Task>> = taskDao.getTasksForNote(noteId)

    /**
     * Gets a task by its ID.
     * @param taskId The ID of the task to retrieve
     * @return The task with the specified ID, or null if not found
     */
    suspend fun getTaskById(taskId: Long): Task? = taskDao.getTaskById(taskId)

    /**
     * Creates a new task from a note.
     * @param noteId The ID of the note to create the task from
     * @param title The title of the task
     * @param description The description of the task
     * @param dueDate The due date of the task (optional)
     * @param priority The priority of the task (1-5, with 5 being highest)
     * @return The ID of the newly created task
     */
    suspend fun createTask(
        noteId: Long,
        title: String,
        description: String = "",
        dueDate: LocalDateTime? = null,
        priority: Int = 3
    ): Long {
        val task = Task(
            noteId = noteId,
            title = title,
            description = description,
            dueDate = dueDate,
            priority = priority.coerceIn(1, 5)
        )
        return taskDao.insert(task)
    }

    /**
     * Updates an existing task.
     * @param task The task to update
     */
    suspend fun updateTask(task: Task) {
        taskDao.update(task)
    }

    /**
     * Updates the completion status of a task.
     * @param taskId The ID of the task to update
     * @param isCompleted Whether the task is completed
     */
    suspend fun updateTaskCompletion(taskId: Long, isCompleted: Boolean) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updatedTask = if (isCompleted != task.isCompleted) {
            task.toggleCompleted()
        } else {
            task.copy(updatedAt = LocalDateTime.now())
        }
        taskDao.update(updatedTask)
    }

    /**
     * Updates the priority of a task.
     * @param taskId The ID of the task to update
     * @param priority The new priority (1-5, with 5 being highest)
     */
    suspend fun updateTaskPriority(taskId: Long, priority: Int) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updatedTask = task.withPriority(priority)
        taskDao.update(updatedTask)
    }

    /**
     * Updates the due date of a task.
     * @param taskId The ID of the task to update
     * @param dueDate The new due date, or null to remove the due date
     */
    suspend fun updateTaskDueDate(taskId: Long, dueDate: LocalDateTime?) {
        val task = taskDao.getTaskById(taskId) ?: return
        val updatedTask = task.withDueDate(dueDate)
        taskDao.update(updatedTask)
    }

    /**
     * Deletes a task.
     * @param task The task to delete
     */
    suspend fun deleteTask(task: Task) {
        taskDao.delete(task)
    }

    /**
     * Deletes a task by its ID.
     * @param taskId The ID of the task to delete
     */
    suspend fun deleteTaskById(taskId: Long) {
        val task = taskDao.getTaskById(taskId) ?: return
        taskDao.delete(task)
    }

    /**
     * Deletes all completed tasks.
     */
    suspend fun deleteCompletedTasks() {
        taskDao.deleteCompletedTasks()
    }

    /**
     * Gets all incomplete tasks with a due date before or on the specified date.
     * @param date The date to check against
     * @return A Flow emitting the list of due tasks
     */
    fun getDueTasks(date: LocalDateTime): Flow<List<Task>> = taskDao.getDueTasks(date)

    /**
     * Gets all incomplete tasks with high priority (4-5).
     * @return A Flow emitting the list of high priority tasks
     */
    fun getHighPriorityTasks(): Flow<List<Task>> = taskDao.getHighPriorityTasks()

    /**
     * Gets all completed tasks.
     * @return A Flow emitting the list of completed tasks
     */
    fun getCompletedTasks(): Flow<List<Task>> = taskDao.getCompletedTasks()

    /**
     * Gets the count of incomplete tasks.
     * @return A Flow emitting the count of incomplete tasks
     */
    fun getIncompleteTaskCount(): Flow<Int> = taskDao.getIncompleteTaskCount()

    /**
     * Gets task statistics for a specific note.
     * @param noteId The ID of the note
     * @return A Flow emitting a map with "total" and "completed" task counts
     */
    fun getTaskStatsForNote(noteId: Long): Flow<Map<String, Int>> = taskDao.getTaskStatsForNote(noteId)
}
