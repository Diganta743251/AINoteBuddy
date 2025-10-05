package com.ainotebuddy.app.offline

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for conflict history management
 */
@Dao
interface ConflictHistoryDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(conflict: ConflictHistoryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(conflicts: List<ConflictHistoryEntity>)
    
    @Update
    suspend fun update(conflict: ConflictHistoryEntity)
    
    @Delete
    suspend fun delete(conflict: ConflictHistoryEntity)
    
    @Query("SELECT * FROM conflict_history WHERE id = :id")
    suspend fun getById(id: String): ConflictHistoryEntity?
    
    @Query("SELECT * FROM conflict_history WHERE entityType = :entityType AND entityId = :entityId ORDER BY resolvedAt DESC")
    fun getConflictHistory(entityType: String, entityId: String): Flow<List<ConflictHistoryEntity>>
    
    @Query("SELECT * FROM conflict_history ORDER BY resolvedAt DESC LIMIT :limit")
    fun getRecentConflicts(limit: Int = 50): Flow<List<ConflictHistoryEntity>>
    
    @Query("SELECT * FROM conflict_history WHERE resolution = :resolution ORDER BY resolvedAt DESC")
    fun getConflictsByResolution(resolution: String): Flow<List<ConflictHistoryEntity>>
    
    @Query("SELECT * FROM conflict_history WHERE isResolved = 0 ORDER BY resolvedAt DESC")
    fun getUnresolvedConflicts(): Flow<List<ConflictHistoryEntity>>
    
    @Query("SELECT * FROM conflict_history WHERE entityId = :entityId AND isResolved = 0 ORDER BY resolvedAt DESC")
    suspend fun getUnresolvedConflictsByEntityId(entityId: String): List<ConflictHistoryEntity>
    
    @Query("SELECT COUNT(*) FROM conflict_history WHERE resolvedAt > :since")
    suspend fun getConflictCountSince(since: Long): Int
    
    @Query("SELECT AVG(confidence) FROM conflict_history WHERE resolution = 'AUTO_MERGE' AND resolvedAt > :since")
    suspend fun getAverageAutoMergeConfidence(since: Long): Float?
    
    @Query("UPDATE conflict_history SET isResolved = 1 WHERE id = :id")
    suspend fun markAsResolved(id: String)
    
    @Query("DELETE FROM conflict_history WHERE resolvedAt < :olderThan")
    suspend fun cleanupOldConflicts(olderThan: Long): Int
    
    @Query("DELETE FROM conflict_history WHERE id = :id")
    suspend fun deleteById(id: String)
}