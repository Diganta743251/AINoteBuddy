package com.ainotebuddy.app.data.dao

import androidx.room.*
import com.ainotebuddy.app.data.model.Task
import com.ainotebuddy.app.repository.TaskCount
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for the Task entity.
 */
@Dao
interface TaskDao {
    /**
     * Get all tasks for a specific note.
     */
    @Query("SELECT * FROM tasks WHERE noteId = :noteId ORDER BY createdAt DESC")
    fun getTasksByNoteId(noteId: Long): Flow<List<Task>>

    /**
     * Get task counts (total and completed) for all notes.
     */
    @Query("""
        SELECT noteId, 
               COUNT(*) as total, 
               SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed 
        FROM tasks 
        GROUP BY noteId
    """)
    fun getTaskCounts(): Flow<Map<Long, TaskCount>>

    /**
     * Get task counts for a specific note.
     */
    @Query("""
        SELECT COUNT(*) as total, 
               SUM(CASE WHEN isCompleted = 1 THEN 1 ELSE 0 END) as completed 
        FROM tasks 
        WHERE noteId = :noteId
    """)
    suspend fun getTaskCountForNote(noteId: Long): TaskCount?

    /**
     * Insert a new task.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: Task): Long

    /**
     * Update an existing task.
     */
    @Update
    suspend fun update(task: Task)

    /**
     * Delete a task.
     */
    @Delete
    suspend fun delete(task: Task)

    /**
     * Delete all tasks for a specific note.
     */
    @Query("DELETE FROM tasks WHERE noteId = :noteId")
    suspend fun deleteTasksForNote(noteId: Long)

    /**
     * Delete all completed tasks for a specific note.
     */
    @Query("DELETE FROM tasks WHERE noteId = :noteId AND isCompleted = 1")
    suspend fun deleteCompletedTasks(noteId: Long)
    
    /**
     * Delete all tasks from the database.
     */
    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()
}
