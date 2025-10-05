package com.ainotebuddy.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.ainotebuddy.app.analytics.TagUsage

@Dao
interface TagUsageDao {
    @Query("SELECT * FROM tag_usage ORDER BY timestamp DESC")
    fun getAllTagUsage(): Flow<List<TagUsage>>
    
    @Query("SELECT * FROM tag_usage WHERE tagName = :tagName ORDER BY timestamp DESC")
    fun getUsageForTag(tagName: String): Flow<List<TagUsage>>
    
    @Query("SELECT * FROM tag_usage WHERE noteId = :noteId ORDER BY timestamp DESC")
    fun getUsageForNote(noteId: Long): Flow<List<TagUsage>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsage(usage: TagUsage): Long
    
    @Query("DELETE FROM tag_usage WHERE timestamp < :olderThan")
    suspend fun deleteOldUsage(olderThan: Long)
    
    @Query("DELETE FROM tag_usage")
    suspend fun clearAllUsage()
}