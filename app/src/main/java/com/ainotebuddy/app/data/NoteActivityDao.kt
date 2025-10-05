package com.ainotebuddy.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ainotebuddy.app.analytics.NoteActivity

@Dao
interface NoteActivityDao {
    @Query("SELECT * FROM note_activities ORDER BY timestamp DESC")
    fun getAllActivities(): Flow<List<NoteActivity>>
    
    @Query("SELECT * FROM note_activities WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getActivitiesForNote(noteId: Long): Flow<List<NoteActivity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertActivity(activity: NoteActivity): Long
    
    @Query("DELETE FROM note_activities WHERE timestamp < :olderThan")
    suspend fun deleteOldActivities(olderThan: Long)
    
    @Query("DELETE FROM note_activities")
    suspend fun clearAllActivities()
}