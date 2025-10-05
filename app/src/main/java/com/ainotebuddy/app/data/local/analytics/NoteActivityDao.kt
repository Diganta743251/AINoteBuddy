package com.ainotebuddy.app.data.local.analytics

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface NoteActivityDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(activity: NoteActivityEntity)

    @Query("SELECT * FROM note_activities WHERE timestamp BETWEEN :startDate AND :endDate ORDER BY timestamp DESC")
    fun getActivitiesInRange(
        startDate: LocalDateTime,
        endDate: LocalDateTime
    ): Flow<List<NoteActivityEntity>>

    @Query("SELECT * FROM note_activities WHERE noteId = :noteId ORDER BY timestamp DESC")
    suspend fun getActivitiesForNote(noteId: Long): List<NoteActivityEntity>

    @Query("""
        SELECT 
            date(timestamp / 1000, 'unixepoch') as date,
            strftime('%H', timestamp / 1000, 'unixepoch') as hour,
            COUNT(*) as count
        FROM note_activities
        WHERE date BETWEEN :startDate AND :endDate
        GROUP BY date, hour
    """)
    suspend fun getActivityHeatmap(startDate: String, endDate: String): List<ActivityHeatmapResult>

    @Query("DELETE FROM note_activities WHERE timestamp < :cutoffTime")
    suspend fun deleteOldActivities(cutoffTime: Long)

    @Query("DELETE FROM note_activities WHERE noteId = :noteId")
    suspend fun deleteActivitiesForNote(noteId: Long)
}