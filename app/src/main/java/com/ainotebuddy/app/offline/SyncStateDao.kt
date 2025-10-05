package com.ainotebuddy.app.offline

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for sync state management
 */
@Dao
interface SyncStateDao {
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(syncState: SyncStateEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(syncStates: List<SyncStateEntity>)
    
    @Update
    suspend fun update(syncState: SyncStateEntity)
    
    @Delete
    suspend fun delete(syncState: SyncStateEntity)
    
    @Query("SELECT * FROM sync_state WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun getSyncState(entityType: String, entityId: String): SyncStateEntity?
    
    @Query("SELECT * FROM sync_state WHERE entityType = :entityType")
    fun getSyncStatesByType(entityType: String): Flow<List<SyncStateEntity>>
    
    @Query("SELECT * FROM sync_state WHERE syncStatus = :status")
    fun getSyncStatesByStatus(status: String): Flow<List<SyncStateEntity>>
    
    @Query("SELECT * FROM sync_state WHERE syncStatus = 'PENDING' ORDER BY lastSyncedAt ASC")
    fun getPendingSyncStates(): Flow<List<SyncStateEntity>>
    
    @Query("SELECT * FROM sync_state WHERE syncStatus = 'CONFLICT'")
    fun getConflictedSyncStates(): Flow<List<SyncStateEntity>>
    
    @Query("UPDATE sync_state SET syncStatus = :status WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun updateSyncStatus(entityType: String, entityId: String, status: String)
    
    @Query("UPDATE sync_state SET localVersion = :version WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun updateLocalVersion(entityType: String, entityId: String, version: Long)
    
    @Query("UPDATE sync_state SET remoteVersion = :version WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun updateRemoteVersion(entityType: String, entityId: String, version: Long)
    
    @Query("DELETE FROM sync_state WHERE entityType = :entityType AND entityId = :entityId")
    suspend fun deleteSyncState(entityType: String, entityId: String)
    
    @Query("SELECT COUNT(*) FROM sync_state WHERE syncStatus = 'PENDING'")
    suspend fun getPendingSyncCount(): Int
    
    @Query("SELECT COUNT(*) FROM sync_state WHERE syncStatus = 'CONFLICT'")
    suspend fun getConflictCount(): Int
}