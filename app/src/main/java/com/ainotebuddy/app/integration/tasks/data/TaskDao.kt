package com.ainotebuddy.app.integration.tasks.data

import androidx.room.*
import com.ainotebuddy.app.integration.tasks.model.Task
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

/**
 * Data Access Object for the Task entity.
 */
@Dao
interface TaskDao {
    /**
     * Inserts a new task into the database.
     * @param task The task to insert
     * @return The ID of the newly inserted task
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    /**
     * Updates an existing task in the database.
     * @param task The task to update
     */
    @Update
    suspend fun update(task: Task)

    /**
     * Deletes a task from the database.
     * @param task The task to delete
     */
    @Delete
    suspend fun delete(task: Task)

    /**
     * Gets a task by its ID.
     * @param taskId The ID of the task to retrieve
     * @return The task with the specified ID, or null if not found
     */
    @Query("SELECT * FROM tasks WHERE id = :taskId")
    suspend fun getTaskById(taskId: Long): Task?

    /**
     * Gets all tasks ordered by due date (with nulls last) and priority.
     * @return A Flow emitting the list of all tasks
     */
    @Query("""
        SELECT * FROM tasks 
        ORDER BY 
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            priority DESC,
            createdAt DESC
    """)
    fun getAllTasks(): Flow<List<Task>>

    /**
     * Gets all tasks for a specific note.
     * @param noteId The ID of the note
     * @return A Flow emitting the list of tasks for the note
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE noteId = :noteId
        ORDER BY 
            isCompleted ASC,
            CASE WHEN dueDate IS NULL THEN 1 ELSE 0 END,
            dueDate ASC,
            priority DESC,
            createdAt DESC
    """)
    fun getTasksForNote(noteId: Long): Flow<List<Task>>

    /**
     * Gets all incomplete tasks with a due date before or on the specified date.
     * @param date The date to check against
     * @return A Flow emitting the list of due tasks
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        AND dueDate <= :date
        ORDER BY dueDate ASC, priority DESC
    """)
    fun getDueTasks(date: LocalDateTime): Flow<List<Task>>

    /**
     * Gets all incomplete tasks with high priority (4-5).
     * @return A Flow emitting the list of high priority tasks
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 0 
        AND priority >= 4
        ORDER BY priority DESC, dueDate ASC
    """)
    fun getHighPriorityTasks(): Flow<List<Task>>

    /**
     * Gets all completed tasks.
     * @return A Flow emitting the list of completed tasks
     */
    @Query("""
        SELECT * FROM tasks 
        WHERE isCompleted = 1
        ORDER BY completedAt DESC
    """)
    fun getCompletedTasks(): Flow<List<Task>>

    /**
     * Deletes all completed tasks.
     */
    @Query("DELETE FROM tasks WHERE isCompleted = 1")
    suspend fun deleteCompletedTasks()

    /**
     * Gets the count of incomplete tasks.
     * @return A Flow emitting the count of incomplete tasks
     */
    @Query("SELECT COUNT(*) FROM tasks WHERE isCompleted = 0")
    fun getIncompleteTaskCount(): Flow<Int>

    /**
     * Gets the count of tasks for a specific note.
     * @param noteId The ID of the note
     * @return A Flow emitting a pair of (total tasks, completed tasks) for the note
     */
    @Query("""
        SELECT 
            COUNT(*) as total,
            SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed
        FROM tasks 
        WHERE noteId = :noteId
    """)
    fun getTaskStatsForNote(noteId: Long): Flow<Map<String, Int>>
}
