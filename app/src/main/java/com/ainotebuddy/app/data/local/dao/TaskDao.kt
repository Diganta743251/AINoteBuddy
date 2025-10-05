package com.ainotebuddy.app.data.local.dao

import androidx.room.*
import com.ainotebuddy.app.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE noteId = :noteId ORDER BY position ASC")
    fun getTasksByNoteId(noteId: Long): Flow<List<Task>>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE noteId = :noteId AND isCompleted = 1")
    fun getCompletedTaskCount(noteId: Long): Flow<Int>
    
    @Query("SELECT COUNT(*) FROM tasks WHERE noteId = :noteId")
    fun getTotalTaskCount(noteId: Long): Flow<Int>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long
    
    @Update
    suspend fun updateTask(task: Task)
    
    @Delete
    suspend fun deleteTask(task: Task)
    
    @Query("DELETE FROM tasks WHERE noteId = :noteId")
    suspend fun deleteTasksForNote(noteId: Long)
    
    @Query("UPDATE tasks SET position = position + 1 WHERE noteId = :noteId AND position >= :position")
    suspend fun incrementPositions(noteId: Long, position: Int)
    
    @Query("UPDATE tasks SET position = position - 1 WHERE noteId = :noteId AND position > :position")
    suspend fun decrementPositions(noteId: Long, position: Int)
}
